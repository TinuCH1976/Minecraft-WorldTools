/*
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

package com.github.rakama.worldtools.data;

import com.mojang.nbt.CompoundTag;
import com.mojang.nbt.DoubleTag;
import com.mojang.nbt.IntTag;
import com.mojang.nbt.ListTag;
import com.mojang.nbt.StringTag;
import com.mojang.nbt.Tag;

public abstract class TileEntity
{
    protected CompoundTag tag;
    
    protected TileEntity(CompoundTag tag)
    {
        this.tag = tag;
    }
    
    public String getID()
    {
        return ((StringTag)tag.get("id")).data;
    }

    @SuppressWarnings("unchecked")
    protected void translate(int x, int y, int z)
    {
        ((IntTag)tag.get("x")).data += x;
        ((IntTag)tag.get("y")).data += y;
        ((IntTag)tag.get("z")).data += z;
        
        if(getID().equals("MobSpawner"))
        {
            CompoundTag data = ((CompoundTag)tag.get("SpawnData"));
            if(data != null)
            {
                Tag temp = data.get("Pos");                
                if(temp != null && (temp instanceof ListTag<?>))
                {
                    ListTag<DoubleTag> pos = (ListTag<DoubleTag>)temp;
                    pos.get(0).data += x;
                    pos.get(1).data += y;
                    pos.get(2).data += z;
                }
            }
        }
    }
    
    public int getX()
    {
        return ((IntTag)tag.get("x")).data;
    }

    public int getY()
    {
        return ((IntTag)tag.get("y")).data;
    }

    public int getZ()
    {
        return ((IntTag)tag.get("z")).data;
    }
    
    protected CompoundTag getTag()
    {
        return tag;
    }
    
    public TileEntity clone(int x, int y, int z)
    {
        TileEntity clone = clone();
        clone.translate(x, y, z);
        return clone;
    }
    
    public abstract TileEntity clone();
}