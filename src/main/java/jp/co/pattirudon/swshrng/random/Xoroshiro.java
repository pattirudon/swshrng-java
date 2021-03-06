package jp.co.pattirudon.swshrng.random;

public class Xoroshiro {
    public static final long XOROSHIRO_CONST = 0x82A2B175229D6A5BL;
    public int i = 0;

    public final long[] s = { 0L, 0L };

    static long rotl(long x, int k) {
        return (x << k) | (x >>> (64 - k));
    }

    public Xoroshiro(long seed) {
        s[0] = seed;
        s[1] = XOROSHIRO_CONST;
    }

    public Xoroshiro(long s0, long s1) {
        s[0] = s0;
        s[1] = s1;
    }

    public long next() {
        long s0 = s[0];
        long s1 = s[1];
        long result = s0 + s1;

        s1 ^= s0;
        s[0] = rotl(s0, 24) ^ s1 ^ (s1 << 16);
        s[1] = rotl(s1, 37);
        i++;
        return result;
    }

    public int nextInt() {
        return (int) next();
    }

    public static long getMask(long x) {
        x--;
        for (int i = 0; i < 64; i++) {
            x |= x >>> (1 << i);
        }
        return x;
    }

    public long random(long N) {
        long mask = getMask(N);
        long result;
        while (true) {
            result = next() & mask;
            if (result < N) {
                return result;
            }
        }
    }
}
