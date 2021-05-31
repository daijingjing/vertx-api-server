package com.ranqiyun.service.web.util;

import io.vertx.core.json.JsonObject;
import org.joda.time.DateTime;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Random;

/**
 * Created by daijingjing on 2018/4/27.
 */
public class ValidateCode {

    private static Font baseFont;

    static {
        baseFont = new Font("Arial", Font.PLAIN,12);
    }

    // 图片的宽度。
    private int width = 200;
    // 图片的高度。
    private int height = 80;
    // 验证码字符个数
    private int codeCount = 4;
    // 验证码干扰线数
    private int lineCount = 100;
    // 验证码
    private String code = null;
    // 验证码图片Buffer
    private BufferedImage buffImg = null;
    private Random generator = new Random();


    private static final char[] codeSequence = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '2', '4', '5', '6', '7', '9'};

    /**
     * 创建默认配置的图形验证码生成类
     */
    public ValidateCode() {
        this.refreshCode();
    }

    /**
     * 指定图片宽高生成图形验证码
     *
     * @param width  图片宽
     * @param height 图片高
     */
    public ValidateCode(int width, int height) {
        this.width = width;
        this.height = height;
        this.refreshCode();
    }

    /**
     * 按指定配置生成图形验证码
     *
     * @param width     图片宽
     * @param height    图片高
     * @param codeCount 字符个数
     * @param lineCount 干扰线条数
     */
    public ValidateCode(int width, int height, int codeCount, int lineCount) {
        this.width = width;
        this.height = height;
        this.codeCount = codeCount;
        this.lineCount = lineCount;
        this.refreshCode();
    }

    /**
     * 重新生成图形验证码
     */
    public void refreshCode() {
        int x = 0, fontHeight = 0, codeY = 0;

        x = width / (codeCount + 2);//每个字符的宽度
        fontHeight = height - 14;//字体的高度
        codeY = height - 7;

        // 图像buffer
        buffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffImg.createGraphics();
        // 生成随机数
        Random random = new Random();
        // 将图像填充为白色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        // 创建字体
        Font font = getFont(fontHeight);
        g.setFont(font);

        // randomCode记录随机产生的验证码
        StringBuffer randomCode = new StringBuffer();
        // 随机产生codeCount个字符的验证码。
        for (int i = 0; i < codeCount; i++) {
            String strRand = String.valueOf(codeSequence[random.nextInt(codeSequence.length)]);
            // 产生随机的颜色值，让输出的每个字符的颜色值都将不同。
            g.setColor(getRandColor(80));
            g.drawString(strRand, (i + 1) * x, codeY);
            // 将产生的四个随机数组合在一起。
            randomCode.append(strRand);
        }
        // 将四位数字的验证码保存到Session中。
        code = randomCode.toString();


        for (int i = 0; i < lineCount / 2; i++) {
            int xs = random.nextInt(width);
            int ys = random.nextInt(height);
            int xe = xs + random.nextInt(width / 8);
            int ye = ys + random.nextInt(height / 8);
            g.setColor(getRandColor(255));
            g.drawLine(xs, ys, xe, ye);
        }

        shear(g, width, height, Color.WHITE);

        for (int i = 0; i < lineCount / 2; i++) {
            int xs = random.nextInt(width);
            int ys = random.nextInt(height);
            int xe = xs + random.nextInt(width / 8);
            int ye = ys + random.nextInt(height / 8);
            g.setColor(getRandColor(255));
            g.drawLine(xs, ys, xe, ye);
        }
    }

    private Color getRandColor(int max) {
        Random random = new Random();
        int red = random.nextInt(max);
        int green = random.nextInt(max);
        int blue = random.nextInt(max);
        return new Color(red, green, blue);
    }

    private void shear(Graphics g, int w1, int h1, Color color) {
        shearX(g, w1, h1, color);
        shearY(g, w1, h1, color);
    }

    private void shearX(Graphics g, int w1, int h1, Color color) {

        int period = generator.nextInt(2);

        boolean borderGap = true;
        int frames = 1;
        int phase = generator.nextInt(2);

        for (int i = 0; i < h1; i++) {
            double d = (double) (period >> 1)
                * Math.sin((double) i / (double) period
                + (6.2831853071795862D * (double) phase)
                / (double) frames);
            g.copyArea(0, i, w1, 1, (int) d, 0);
            if (borderGap) {
                g.setColor(color);
                g.drawLine((int) d, i, 0, i);
                g.drawLine((int) d + w1, i, w1, i);
            }
        }

    }

    private void shearY(Graphics g, int w1, int h1, Color color) {

        int period = generator.nextInt(40) + 10; // 50;

        boolean borderGap = true;
        int frames = 20;
        int phase = 7;
        for (int i = 0; i < w1; i++) {
            double d = (double) (period >> 1)
                * Math.sin((double) i / (double) period
                + (6.2831853071795862D * (double) phase)
                / (double) frames);
            g.copyArea(i, 0, 1, h1, 0, (int) d);
            if (borderGap) {
                g.setColor(color);
                g.drawLine(i, (int) d, i, 0);
                g.drawLine(i, (int) d + h1, i, h1);
            }

        }

    }

    private Font getFont(int fontHeight) {
        return baseFont.deriveFont(Font.PLAIN, fontHeight);
    }

    /**
     * 写图形验证码到指定文件中
     *
     * @param path 文件路径
     * @throws IOException
     */
    public void write(String path) throws IOException {
        OutputStream sos = new FileOutputStream(path);
        this.write(sos);
    }

    /**
     * 输出图形验证码到指定流
     *
     * @param sos
     * @throws IOException
     */
    public void write(OutputStream sos) throws IOException {
        ImageIO.write(buffImg, "gif", sos);
        sos.close();
    }

    /**
     * 获取图形验证码Buffer
     *
     * @return
     */
    public BufferedImage getBuffImg() {
        return buffImg;
    }

    /**
     * 获取随机生成的验证码
     *
     * @return 生成的验证码
     */
    public String getSign() {
        try {
            return DesUtil.encrypt(new JsonObject().put("code", code.toLowerCase()).put("ts", DateTime.now().plusMinutes(1).toString()).encode());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean validateCode(String sign, String code) {
        JsonObject ss = null;
        try {
            ss = new JsonObject(DesUtil.decrypt(sign));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Objects.isNull(ss))
            return false;

        if (DateTime.parse(ss.getString("ts")).isBefore(DateTime.now()))
            return false;

        return code.toLowerCase().equals(ss.getString("code"));
    }
}
