package ru.shitneba.gfx;

import java.io.ByteArrayOutputStream;

/**
 * Простой кодировщик анимированного GIF (3.9.0, «лучший момент»).
 * Общая палитра 6×7×6 цветов, упорядоченный дизеринг 4×4, сжатие LZW.
 */
public final class GifEnc {
    static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    /**
     * frames — кадры RGBA (w*h*4), строки снизу вверх (как отдаёт glReadPixels).
     * delayCs — пауза между кадрами в сотых секунды.
     */
    public static byte[] encode(byte[][] frames, int n, int w, int h, int delayCs) {
        ByteArrayOutputStream o = new ByteArrayOutputStream(1 << 20);
        writeStr(o, "GIF89a");
        le16(o, w); le16(o, h);
        o.write(0xF7); // глобальная палитра 256 цветов
        o.write(0); o.write(0);
        for (int i = 0; i < 256; i++) {
            if (i < 252) {
                int r = i / 42, g = (i / 6) % 7, b = i % 6;
                o.write(r * 255 / 5); o.write(g * 255 / 6); o.write(b * 255 / 5);
            } else { o.write(0); o.write(0); o.write(0); }
        }
        // бесконечный повтор
        o.write(0x21); o.write(0xFF); o.write(11); writeStr(o, "NETSCAPE2.0"); o.write(3); o.write(1); le16(o, 0); o.write(0);
        byte[] idx = new byte[w * h];
        for (int f = 0; f < n; f++) {
            byte[] px = frames[f];
            for (int y = 0; y < h; y++) {
                int src = (h - 1 - y) * w * 4, dst = y * w;
                for (int x = 0; x < w; x++, src += 4) {
                    float t = (BAYER[(y & 3) * 4 + (x & 3)] + 0.5f) / 16f - 0.5f;
                    int r = q(px[src] & 0xFF, 5, t), g = q(px[src + 1] & 0xFF, 6, t), b = q(px[src + 2] & 0xFF, 5, t);
                    idx[dst + x] = (byte) (r * 42 + g * 6 + b);
                }
            }
            o.write(0x21); o.write(0xF9); o.write(4); o.write(0); le16(o, delayCs); o.write(0); o.write(0);
            o.write(0x2C); le16(o, 0); le16(o, 0); le16(o, w); le16(o, h); o.write(0);
            lzw(o, idx, 8);
        }
        o.write(0x3B);
        return o.toByteArray();
    }

    static int q(int v, int levels, float t) {
        int k = Math.round(v * levels / 255f + t);
        return k < 0 ? 0 : (k > levels ? levels : k);
    }

    static void writeStr(ByteArrayOutputStream o, String s) { for (int i = 0; i < s.length(); i++) o.write(s.charAt(i)); }

    static void le16(ByteArrayOutputStream o, int v) { o.write(v & 0xFF); o.write((v >> 8) & 0xFF); }

    // ---------- LZW (вариант GIF: коды переменной длины, младшие биты вперёд) ----------
    static final class Bits {
        final ByteArrayOutputStream o; final byte[] blk = new byte[255]; int bn, acc, accBits;
        Bits(ByteArrayOutputStream o) { this.o = o; }
        void put(int code, int len) {
            acc |= code << accBits; accBits += len;
            while (accBits >= 8) { byteOut(acc & 0xFF); acc >>>= 8; accBits -= 8; }
        }
        void byteOut(int b) { blk[bn++] = (byte) b; if (bn == 255) flush(); }
        void flush() { if (bn > 0) { o.write(bn); o.write(blk, 0, bn); bn = 0; } }
        void end() { if (accBits > 0) byteOut(acc & 0xFF); acc = 0; accBits = 0; flush(); o.write(0); }
    }

    static void lzw(ByteArrayOutputStream o, byte[] data, int minBits) {
        o.write(minBits);
        int clear = 1 << minBits, eoi = clear + 1;
        // словарь: ключ (префикс << 8 | символ) → код, открытая адресация
        final int HS = 5003 * 2;
        int[] keys = new int[HS], vals = new int[HS];
        Bits b = new Bits(o);
        int codeLen = minBits + 1, next = eoi + 1;
        java.util.Arrays.fill(keys, -1);
        b.put(clear, codeLen);
        int prefix = data[0] & 0xFF;
        for (int i = 1; i < data.length; i++) {
            int c = data[i] & 0xFF;
            int key = (prefix << 8) | c;
            int h = (key * 31 + c) & 0x7FFFFFFF; h %= HS;
            boolean found = false;
            while (keys[h] != -1) {
                if (keys[h] == key) { prefix = vals[h]; found = true; break; }
                h = (h + 1) % HS;
            }
            if (found) continue;
            b.put(prefix, codeLen);
            if (next < 4096) {
                keys[h] = key; vals[h] = next++;
                if (next > (1 << codeLen) && codeLen < 12) codeLen++;
            } else {
                b.put(clear, codeLen);
                java.util.Arrays.fill(keys, -1);
                codeLen = minBits + 1; next = eoi + 1;
            }
            prefix = c;
        }
        b.put(prefix, codeLen);
        b.put(eoi, codeLen);
        b.end();
    }

    private GifEnc() { }
}
