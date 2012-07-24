package com.github.rakama.minecraft.tools.light;

import com.github.rakama.minecraft.chunk.Block;
import com.github.rakama.minecraft.chunk.Chunk;
import com.github.rakama.minecraft.chunk.Section;
import com.github.rakama.util.CircularBuffer;

/**
 * Copyright (c) 2012, RamsesA <ramsesakama@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

class LightCache
{
    protected final int width, height, length;
    protected final int width16, height16, length16;
    protected final int width16round, height16round, length16round;
    protected final int wscale, lscale;
    protected final Section[] sections;
    protected final Chunk[] chunks;

    protected boolean blocklight;
    protected final int yoff, zoff, xmask, ymask, zmask;

    protected LightCache(int width16, int length16)
    {
        if(width16 <= 0 || length16 <= 0)
            throw new IllegalArgumentException();

        this.wscale = intLog2(width16);
        this.lscale = intLog2(length16);
        this.width16 = width16;
        this.height16 = Chunk.num_sections;
        this.length16 = length16;
        this.width = width16 << 4;
        this.height = height16 << 4;
        this.length = length16 << 4;

        yoff = wscale + lscale;
        zoff = wscale;
        xmask = bitmask(wscale);
        zmask = bitmask(lscale);
        ymask = bitmask(32 - wscale - lscale);

        width16round = 1 << wscale;
        length16round = 1 << lscale;
        height16round = height16;

        sections = new Section[width16round * length16round * height16round];
        chunks = new Chunk[width16round * length16round];
    }

    protected void setChunk(int x16, int z16, Chunk chunk)
    {
        int cindex = toChunkIndex(x16, z16);
        chunks[cindex] = chunk;

        if(chunk != null)
        {
            for(int sec = 0; sec < Chunk.num_sections; sec++)
            {
                int sindex = toSectionIndex(x16, sec, z16);
                sections[sindex] = chunk.getSection(sec);
            }
        }
        else
        {
            for(int sec = 0; sec < Chunk.num_sections; sec++)
            {
                int sindex = toSectionIndex(x16, sec, z16);
                sections[sindex] = null;
            }
        }
    }

    protected void setLight(int x, int y, int z, byte val)
    {
        int sindex = toSectionIndex(x >> 4, y >> 4, z >> 4);
        Section sec = sections[sindex];

        if(sec == null)
            return;

        int eindex = toElementIndex(x & 0xF, y & 0xF, z & 0xF);

        if(blocklight)
            sec.blocklight.set(eindex, val);
        else
            sec.skylight.set(eindex, val);
    }

    protected int getLight(int x, int y, int z)
    {
        int sindex = toSectionIndex(x >> 4, y >> 4, z >> 4);
        Section sec = sections[sindex];

        if(sec == null)
            return 15;

        int eindex = toElementIndex(x & 0xF, y & 0xF, z & 0xF);

        if(blocklight)
            return sec.blocklight.get(eindex);
        else
            return sec.skylight.get(eindex);
    }

    protected int getHeight(int x, int z)
    {
        int cindex = toChunkIndex(x >> 4, z >> 4);
        Chunk chunk = chunks[cindex];

        if(chunk == null)
            return 0;

        x &= 0xF;
        z &= 0xF;

        return chunks[cindex].getHeight(x, z);
    }

    protected int getBlockId(int x, int y, int z)
    {
        int sindex = toSectionIndex(x >> 4, y >> 4, z >> 4);
        Section sec = sections[sindex];

        if(sec == null)
            return 0;

        int eindex = toElementIndex(x & 0xF, y & 0xF, z & 0xF);
        return sec.blockid[eindex];
    }

    protected int getBlockLuminance(int x, int y, int z)
    {
        return Block.getLuminance(getBlockId(x, y, z));
    }

    protected int getBlockDiffusion(int x, int y, int z)
    {
        return Block.getDiffusion(getBlockId(x, y, z));
    }
    
    protected boolean isOpaque(int x, int y, int z)
    {
        return Block.isOpaque(getBlockId(x, y, z));
    }

    protected void enqueueSkyLights(CircularBuffer queue)
    {
        for(int z = 0; z < length; z++)
        {
            for(int x = 0; x < width; x++)
            {
                int h = getHeight(x, z);
                int hMax = h;

                if(z > 0)
                    hMax = Math.max(hMax, getHeight(x, z - 1));

                if(z < length - 1)
                    hMax = Math.max(hMax, getHeight(x, z + 1));

                if(x > 0)
                    hMax = Math.max(hMax, getHeight(x - 1, z));

                if(x < width - 1)
                    hMax = Math.max(hMax, getHeight(x + 1, z));

                enqueueColumn(queue, x, z, h, hMax);
            }
        }
    }

    private void enqueueColumn(CircularBuffer queue, int x, int z, int y0, int y1)
    {
        lightColumn(x, y0, z);
        for(int y = y0; y <= y1; y++)
            enqueueBlock(queue, x, y, z, 15);
    }

    private void lightColumn(int x, int y, int z)
    {
        int cindex = toChunkIndex(x >> 4, z >> 4);
        if(chunks[cindex] == null)
            return;

        int maxY = countBottomSections(chunks[cindex]) << 4;

        for(int y0 = y; y0 < maxY; y0++)
            setLight(x, y0, z, (byte) 15);
    }

    private int countBottomSections(Chunk chunk)
    {
        for(int i = 0; i < Chunk.num_sections; i++)
            if(chunk.getSection(i) == null)
                return i;

        return Chunk.num_sections;
    }

    protected void enqueueBlockLights(CircularBuffer queue)
    {
        for(int sindex = 0; sindex < sections.length; sindex++)
        {
            Section sec = sections[sindex];

            if(sec == null)
                continue;

            for(int eindex = 0; eindex < 4096; eindex++)
            {
                int light = (byte) Block.getLuminance(sec.blockid[eindex]);

                if(light > 0)
                {
                    sec.blocklight.set(eindex, light);
                    enqueueBlock(queue, sindex, eindex, light);
                }
            }
        }
    }

    private void enqueueBlock(CircularBuffer queue, int sindex, int eindex, int light)
    {
        int sx = sindex & xmask;
        int sy = (sindex >> yoff) & ymask;
        int sz = (sindex >> zoff) & zmask;

        int ex = eindex & 0xF;
        int ey = (eindex >> 8) & 0xF;
        int ez = (eindex >> 4) & 0xF;

        int x = (sx << 4) + ex;
        int y = (sy << 4) + ey;
        int z = (sz << 4) + ez;

        enqueueBlock(queue, x, y, z, light);
    }

    private void enqueueBlock(CircularBuffer queue, int x, int y, int z, int light)
    {
        queue.push(Relighter.pack(x, y, z, (byte) light));
    }

    private int toChunkIndex(int x, int z)
    {
        return x + (z << zoff);
    }

    private int toSectionIndex(int x, int y, int z)
    {
        return x + (z << zoff) + (y << yoff);
    }

    private int toElementIndex(int x, int y, int z)
    {
        return x + (z << 4) + (y << 8);
    }

    private static int bitmask(int bits)
    {
        int mask = 0;

        for(int i = 0; i < bits; i++)
            mask |= 1 << i;

        return mask;
    }

    private int intLog2(int v)
    {
        return (int) Math.ceil(Math.log(v) / Math.log(2));
    }
}