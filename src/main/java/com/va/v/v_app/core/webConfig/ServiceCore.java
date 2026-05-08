package com.va.v.v_app.core.webConfig;

public class ServiceCore {

    private static ServiceCore instance;

    private ServiceCore() {
    }

    public static ServiceCore getSequencerInstance() {
        if (instance == null) {
            instance = new ServiceCore();
        }
        return instance;
    }

    public String getSequenceId(Object param) {
        return "TXN-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
    }
}
