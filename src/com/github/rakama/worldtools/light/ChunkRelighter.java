package com.github.rakama.worldtools.light;

import com.github.rakama.worldtools.data.Chunk;
import com.github.rakama.worldtools.light.LightCache.Mode;
import com.github.rakama.worldtools.util.CircularBuffer;

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

public class ChunkRelighter
{
    public static final int min_span = 3;
    public static final int max_span = 34;

    protected CircularBuffer queue;
    protected LightCache cache;
    protected LightWrapper[] wrapper;

    protected final int span, width, length, height;

    public ChunkRelighter()
    {
        this(3);
    }
    
    public ChunkRelighter(int span)
    {
        if(span < min_span || span > max_span)
            throw new IllegalArgumentException("span value out of range");

        this.span = span;
        width = span * Chunk.width;
        length = span * Chunk.length;
        height = Chunk.height;

        queue = new CircularBuffer(width * length * height);
        cache = new LightCache(span, span);
        wrapper = new LightWrapper[span * span];

        for(int z = 0; z < span; z++)
            for(int x = 0; x < span; x++)
                if(isImmutable(x, z))
                    wrapper[x + z * span] = new LightWrapper();
    }

    public int getSpan()
    {
        return span;
    }

    public void lightChunks(Chunk[] localChunks)
    {
        if(localChunks.length != span * span)
            throw new IllegalArgumentException("expected array of size " + span * span);

        fillLightCache(localChunks);

        // compute block lights
        queue.clear();
        cache.setMode(Mode.BLOCKLIGHT);
        cache.clearBlockLights();
        cache.enqueueBlockLights(queue);
        propagateLights();

        // compute sky lights
        queue.clear();
        cache.setMode(Mode.SKYLIGHT);
        cache.clearSkyLights();
        cache.enqueueSkyLights(queue);
        propagateLights();
        
        cache.clear();
        clearWrappers();
    }

    protected void fillLightCache(Chunk[] localChunks)
    {
        for(int z = 0; z < span; z++)
        {
            for(int x = 0; x < span; x++)
            {
                Chunk chunk = localChunks[x + z * span];
                
                if(chunk == null)
                {
                    cache.setChunk(x, z, null);
                    continue;
                }

                chunk.trimSections();
                chunk.recomputeHeightmap();
                
                if(isImmutable(x, z))
                    cache.setChunk(x, z, getWrapper(x, z, chunk));
                else
                    cache.setChunk(x, z, localChunks[x + z * span]);
            }            
        }
    }
    
    protected void propagateLights()
    {
        int[] pos = new int[3];

        // perform wavefront propagation
        while(!queue.isEmpty())
        {
            int index = queue.poll();
            byte light = unpack(index, pos);
            int x = pos[0];
            int y = pos[1];
            int z = pos[2];

            light--;
            
            propagateLight(x, y + 1, z, light);
            propagateLight(x, y - 1, z, light);
            propagateLight(x - 1, y, z, light);
            propagateLight(x + 1, y, z, light);
            propagateLight(x, y, z + 1, light);
            propagateLight(x, y, z - 1, light);
        }
    }

    private void propagateLight(int x, int y, int z, byte light)
    {
        // skip if x/y/z is out of bounds
        if(x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= length)
            return;
        
        int diffusion = cache.getBlockDiffusion(x, y, z);
        
        if(diffusion > 0)
        {
            light -= diffusion - 1;
            if(light < 1)
                return;            
        }
            
        if(setLight(x, y, z, light))
            queue.push(pack(x, y, z, light));
    }

    private boolean setLight(int x, int y, int z, byte newLight)
    {
        // get previous light value
        int prevLight = cache.getLight(x, y, z);

        // return false if block has higher light value or if block is opaque
        if(newLight <= prevLight || cache.isOpaque(x, y, z))
            return false;
        
        // update with new light value
        cache.setLight(x, y, z, newLight);

        return true;
    }

    private LightWrapper getWrapper(int x, int z, Chunk chunk)
    {
        if(chunk == null)
            return null;
        
        LightWrapper p = wrapper[x + z * span];
        p.assign(chunk);
        return p;
    }
    
    private void clearWrappers()
    {
        for(int z = 0; z < span; z++)
            for(int x = 0; x < span; x++)
                if(isImmutable(x, z))
                    wrapper[x + z * span].clear();
    }
    
    private boolean isImmutable(int x, int z)
    {
        return z <= 0 || z >= span - 1 || x <= 0 || x >= span - 1;
    }

    protected static int pack(int x, int y, int z, byte light)
    {
        // max x/z = 1023
        // max y = 255
        // max light = 15

        x = x << 22;
        z = z << 12;
        y = y << 4;
        return x | y | z | light;
    }

    protected static byte unpack(int i, int[] coordinates)
    {
        coordinates[0] = (i >> 22) & 0x3FF;
        coordinates[1] = (i >> 4) & 0xFF;
        coordinates[2] = (i >> 12) & 0x3FF;
        return (byte) (i & 0xF);
    }
}