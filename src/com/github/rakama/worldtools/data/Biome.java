package com.github.rakama.worldtools.data;

public class Biome implements Comparable<Biome>
{
    public static final Biome OCEAN = new Biome(0);
    public static final Biome PLAINS = new Biome(1);
    public static final Biome DESERT = new Biome(2);
    public static final Biome MOUNTAINS = new Biome(3);
    public static final Biome FOREST = new Biome(4);
    public static final Biome TAIGA = new Biome(5);
    public static final Biome SWAMP = new Biome(6);
    public static final Biome RIVER = new Biome(7);
    public static final Biome NETHER = new Biome(8);
    public static final Biome SKY = new Biome(9);
    public static final Biome FROZEN_OCEAN = new Biome(10);
    public static final Biome FROZEN_RIVER = new Biome(11);
    public static final Biome ICE_PLAINS = new Biome(12);
    public static final Biome ICE_MOUNTAINS = new Biome(13);
    public static final Biome MUSHROOM_ISLAND = new Biome(14);
    public static final Biome MUSHROOM_SHORE = new Biome(15);
    public static final Biome BEACH = new Biome(16);
    public static final Biome DESERT_HILLS = new Biome(17);
    public static final Biome FOREST_HILLS = new Biome(18);
    public static final Biome TAIGA_HILLS = new Biome(19);
    public static final Biome MOUNTAINS_EDGE = new Biome(20);
    
    protected int id;
    
    protected Biome(int id)
    {
        this.id = id;
    }
        
    public int getID()
    {
        return id;
    }

    public int hashCode()
    {
        return id;
    }
    
    public boolean equals(Object o)
    {
        return equals((Biome)o);
    }
    
    public boolean equals(Biome b)
    {
        return b.id == id;
    }

    public boolean equals(int id)
    {
        return id == this.id;
    }

    public int compareTo(Biome b)
    {
        return id - b.id;
    }
}