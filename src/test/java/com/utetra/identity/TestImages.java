package com.utetra.identity;
final class TestImages {
    private TestImages() {}
    static byte[] png() {
        try {
            var out=new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);
            return out.toByteArray();
        } catch(java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
}
