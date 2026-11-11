package com.company.rbac.auth;

import com.company.rbac.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final Duration EXPIRATION = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public Captcha create() {
        StringBuilder code = new StringBuilder(4);
        for (int index = 0; index < 4; index++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        String id = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("captcha:" + id, code.toString(), EXPIRATION);

        try {
            BufferedImage canvas = new BufferedImage(128, 42, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = canvas.createGraphics();
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(240, 246, 255));
            graphics.fillRect(0, 0, 128, 42);
            for (int index = 0; index < 10; index++) {
                graphics.setColor(new Color(175, 201, 237));
                graphics.drawLine(
                        RANDOM.nextInt(128), RANDOM.nextInt(42),
                        RANDOM.nextInt(128), RANDOM.nextInt(42));
            }
            graphics.setFont(new Font("SansSerif", Font.BOLD, 25));
            for (int index = 0; index < code.length(); index++) {
                graphics.setColor(new Color(
                        35 + RANDOM.nextInt(50),
                        70 + RANDOM.nextInt(55),
                        130 + RANDOM.nextInt(65)));
                AffineTransform transform = graphics.getTransform();
                int x = 12 + index * 28;
                int y = 29 + RANDOM.nextInt(7) - 3;
                graphics.rotate((RANDOM.nextDouble() - 0.5) * 0.35, x, y);
                graphics.drawString(String.valueOf(code.charAt(index)), x, y);
                graphics.setTransform(transform);
            }
            graphics.dispose();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", bytes);
            String image = "data:image/png;base64," + Base64.getEncoder()
                    .encodeToString(bytes.toByteArray());
            return new Captcha(id, image);
        } catch (IOException exception) {
            throw new BusinessException("验证码生成失败");
        }
    }

    public void verify(String id, String input) {
        if (id == null || input == null || id.isBlank() || input.isBlank()
                || !id.matches("[0-9a-f-]{36}")) {
            throw new BusinessException("请输入验证码");
        }
        String expected = redisTemplate.opsForValue().getAndDelete("captcha:" + id);
        if (expected == null || !expected.equalsIgnoreCase(input.trim())) {
            throw new BusinessException("验证码错误或已过期，请重新获取");
        }
    }

    public record Captcha(String id, String image) {
    }
}
