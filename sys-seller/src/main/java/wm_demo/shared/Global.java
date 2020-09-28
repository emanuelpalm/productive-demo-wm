package wm_demo.shared;

public class Global {
    private Global() {}


    public static final String SELLER_HOSTNAME = "sys-seller.uni";
    public static final int SELLER_PORT = 9002;
    public static final String SELLER_KEYSTORE = "keystore.seller.p12";

    public static final String MIDDLEWARE_HOSTNAME = "sys-middleware.uni";
    public static final int MIDDLEWARE_PORT = 9003;
    public static final String MIDDLEWARE_KEYSTORE = "keystore.middleware.p12";

    public static final String SR_HOSTNAME = "service-registry.uni";
    public static final int SR_PORT = 8443;

    public static final String TRUSTSTORE = "truststore.p12";
}
