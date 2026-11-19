package cl.bibliocano.dominio;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas con PBKDF2 (incluido en el JDK, sin dependencias externas).
 *
 * <p>Formato guardado: {@code iteraciones:salBase64:hashBase64}. La sal aleatoria hace que dos
 * bibliotecarios con la misma contraseña tengan hashes distintos, y las iteraciones encarecen un
 * ataque de fuerza bruta si alguien se roba la tabla {@code bibliotecario}.
 */
public final class Contrasenas {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 100_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom AZAR = new SecureRandom();

    private Contrasenas() {
    }

    public static String generarHash(String password) {
        byte[] sal = new byte[BYTES_SAL];
        AZAR.nextBytes(sal);
        byte[] hash = derivar(password, sal, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder();
        return ITERACIONES + ":" + b64.encodeToString(sal) + ":" + b64.encodeToString(hash);
    }

    /** Compara en tiempo constante para no filtrar, por el tiempo de respuesta, cuántos bytes acertó. */
    public static boolean coincide(String password, String guardado) {
        if (password == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split(":");
        if (partes.length != 3) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[0]);
            Base64.Decoder b64 = Base64.getDecoder();
            byte[] sal = b64.decode(partes[1]);
            byte[] esperado = b64.decode(partes[2]);
            return MessageDigest.isEqual(esperado, derivar(password, sal, iteraciones));
        } catch (IllegalArgumentException formatoInvalido) {
            return false;
        }
    }

    private static byte[] derivar(String password, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sal, iteraciones, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            // PBKDF2WithHmacSHA256 es obligatorio en todo JDK: si falta, la plataforma está rota
            throw new IllegalStateException("El JDK no ofrece " + ALGORITMO, e);
        } finally {
            spec.clearPassword();
        }
    }
}
