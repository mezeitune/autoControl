package com.example.macbook.autocontrol.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.example.macbook.autocontrol.udp.UdpClient;
import com.example.macbook.autocontrol.udp.UdpDataHandler;
import com.example.macbook.autocontrol.util.Util;

import java.util.HashMap;


/**
 * Comunicacion entre la aplicacion y un semáforo en particular
 */
public class SemComunication extends BroadcastReceiver implements Runnable, UdpDataHandler,
        TextToSpeech.OnInitListener {

    private static int networkId;
    public static String currentSSID;
    public static StaticState staticState = StaticState.NO_CONECTADO;
    public static StaticState conexion = StaticState.NO_CONECTADO;

    private static HashMap<String, SemComunication> currentComunications = new HashMap<String, SemComunication>();
    public static boolean run = true;
    private TextToSpeech textToSpeech;
    private boolean running = true;
    private WifiManager wifi;
    private WifiConfiguration conf;
    private Context context;
    private String SSID;
    public static UdpClient client;

    private static final int TIEMPO_AVISO = 5;

    // Tiempos y datos del cruce
    int tiempoVerde = 0;
    int tiempoRojo = 0;
    int tiempoDisponible = 0;
    int lastAlert = 100;
    String calleRojo = "";
    String calleVerde = "";
    boolean reproducirCompleto = true;
    private boolean continueAskingTimes = true;
    private boolean canCross = false;

    /**
     * Método estático para la ejecucion concurrente de
     * una comunicación con un semáforo.
     */
    public static void create(String SSID, Context context) {

        if (!SSID.equals(SemComunication.currentSSID)) {

            SemComunication comunication = new SemComunication();

            comunication.wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            comunication.context = context;
            comunication.SSID = SSID;

            new Thread(comunication).start();

        }
    }

    private boolean isConnected() {
        return this.client != null && this.client.isConnected();
    }

    @Override
    public  void run() {

        // Configuro la red wifi correspondiente
        this.configureWiFi();

        // Conecto con la red WiFi
        this.connectWiFi();

    }


    /**
     * Configura la conexión con la red WiFi
     */

    private void configureWiFi() {

        String networkPass = Util.WIFI_PASS;

        conf = new WifiConfiguration();
        conf.SSID = "\"" + SSID + "\"";
        conf.preSharedKey = "\"".concat(networkPass).concat("\"");

        conf.hiddenSSID = true;
        conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        conf.status = WifiConfiguration.Status.ENABLED;

        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

    }

    /**
     * Recibe el SSID de un semaforo y se conecta a este
     */
    public void connectWiFi() {

        // Agrego la red para poder conectarme
        conf.networkId = wifi.addNetwork(conf);
        if (conf.networkId == -1) {
            Log.i("#FSEM# SERVICE", "Fallo addNetwork");
            return;
        }

        // Receiver para cambio de estado de WIFI
        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);

        context.registerReceiver(this, intentFilter);

        // Mientras haya otra conexión con algun semaforo, espero a que se termine.
        while (SemComunication.staticState == StaticState.CONECTADO) {
            Log.i("#FSEM# SERVICE", "Me quiero conectar a " + this.conf.SSID
                    + ", pero sigue conectado a otro semaforo: " + SemComunication.currentSSID);
            // Obligo a desconectarse.
            wifi.disconnect();
            SystemClock.sleep(1000);
        }

        // Me conecto con la red que quiero, y cuando lo logre
        // se ejecuta el receiver anterior
        wifi.disconnect();
        Log.i("#FSEM# SERVICE", "Me intento conectar con " + (this.SSID));
        wifi.enableNetwork(this.conf.networkId, true);
        wifi.reconnect();
        SemComunication.staticState = StaticState.CONECTADO;
        SemComunication.networkId = this.conf.networkId;
        SemComunication.currentSSID = this.SSID;
    }

    /**
     * Implementacion del metodo que recibe cambios en el estado de la señal
     */

    @Override
    public void onReceive(Context context, Intent intent) {

        int numberOfLevels = 4;
        WifiInfo wifiInfo = wifi.getConnectionInfo();

        // Si el cambio se trata de mi red wifi:
        String ssid = wifiInfo.getSSID();

        if (wifiInfo.getSSID() != null && (
                wifiInfo.getSSID().equals(this.SSID)
                        || wifiInfo.getSSID().equals("\"" + this.SSID + "\"")
        )) {

            switch (wifi.getWifiState()) {
                // Majeno la conexion
                case WifiManager.WIFI_STATE_ENABLED:
                    this.conf.networkId = wifiInfo.getNetworkId();
                    SemComunication.networkId = wifiInfo.getNetworkId();

                    // Nivel de poder de la señal.
                    // int level = WifiManager.calculateSignalLevel(
                    //        wifiInfo.getRssi(), numberOfLevels);

                    // La intensidad tiene que ser mayor o igual a 3 lineas.
                    //if (level >= 3) {

                    // Creo el socket
                    if (this.client == null) {
                        // || !this.client.isConnected()) {
                        Log.i("#FSEM# SERVICE", "Me conecte " + wifiInfo.getSSID());

                        createClient();
                    }

                    //} else {
                    // Si la potencia es menor, me desconecto
                    //  wifi.disconnect();
                    //}

                    break;
                // Manejo la desconexion
                case WifiManager.WIFI_STATE_DISABLED:

                    // Desconeccion de la red
                    wifi.disableNetwork(conf.networkId);
                    SemComunication.networkId = -1;
                    SemComunication.currentSSID = null;
                    SemComunication.staticState = StaticState.NO_CONECTADO;

                    context.unregisterReceiver(this);

                    SemComunication.currentComunications.remove(conf.SSID);

                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Crea el cliente TCP, y lo conecta
     */
    public void createClient() {
        client = new UdpClient(Util.semIp, Util.semPort, this);

        // Le paso true porque quiero que lo haga en un hilo nuevo
        client.connect(true);
    }

    /**
     * Cierra todas las conexiones y elimina el cliente
     */
    public void close() {
        if (this.client != null && this.client.isConnected()) {
            this.client.close();
        }
        this.running = false;
    }

    /**
     * Este método se ejecuta cuando el socket ya esta conectado,
     * por lo tanto hay conexión total con el semáforo
     */
    @Override
    public void onConected(boolean ok) {
        if (ok) {

            Log.i("#FSEM# SERVICE", "CONECTADO");
            SemComunication.staticState = StaticState.CONECTADO;
            Log.i("#FSEM# SERVICE", "CAMBIE VALOR");
            SemComunication.currentComunications.put(SSID, this);
            Log.i("#FSEM# SERVICE", "CURRENTCO");

            // Instancio el sintetizador de voz.
            //this.textToSpeech = new TextToSpeech(context, this);
            //this.textToSpeech.setSpeechRate(2);
        } else {
            Log.i("#FSEM# SERVICE", "NO CONECTADO");
            // Si no me pude conectar, actualizo el estado.
            staticDisconect();
            close();
            // DEBUG reintento conexion:
            createClient();
        }

    }

    /**
     * Estoy conectado y con el sintetizador iniciado
     */
    @Override
    public void onInit(int i) {

        // Solicito informacion completa
        requestFull();
        Log.i("#FSEM# SERVICE", "ON INIT");

        // Loop mientras haya conexión para solicitar
        // información actualizada
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (client.isConnected() && SemComunication.run) {
                    informTimeToCross();
                    SystemClock.sleep(900);
                }
                close();
                staticDisconect();
            }
        }).start();
    }

    /**
     * Informa el tiempo para cruzar.
     */
    private void informTimeToCross() {
        if (continueAskingTimes) {
            requestTimes();
        }
    }

    /**
     * Una vez encontrado y conectado el semáforo, se solicita la informacion completa
     */
    public void requestFull() {

        Log.i("#FSEM# SERVICE", "REQUEST FULL");

        client.send("<CTD150>");
    }
    public static  void requestAtras() {

        Log.i("#FSEM# SERVICE", "REQUEST ATRAS");

        client.send("<ATRAS>");
    }

    public static  void requestFrenar() {

        Log.i("#FSEM# SERVICE", "REQUEST FRENAR");

        client.send("<CTD500>");
    }
    public static  void requestAdelante() {

        Log.i("#FSEM# SERVICE", "REQUEST ADELANTE");

        client.send("<ADELANTE>");
    }

    public static  void requestDerecha() {

        Log.i("#FSEM# SERVICE", "REQUEST DERECHA");

        client.send("<DERECHA>");
    }

    public static  void requestIzquierda() {

        Log.i("#FSEM# SERVICE", "REQUEST IZQUIERDA");

        client.send("<IZQUIERDA>");
    }

    public static  void requestVelocidad(float rating) {
        int rating2 = Math.round(rating);
        String number="500";
        switch(rating2){
            case 1:
                number = "150";
                break;
            case 2:
                number = "100";
                break;
            case 3:
                 number= "080";
                break;
            case 4:
                number = "050";
                break;

            case 5:
                number = "020";
                break;


        }
        Log.i("#FSEM# SERVICE", number);
        client.send("<CTD" + number + ">");
    }

    /**
     * Una vez encontrado y conectado el semáforo, se solicita la informacion completa
     */
    protected void requestTimes() {

        Log.i("#FSEM# SERVICE", "REQUEST TIME");

        client.send("<TIEMPO>");

    }


    /**
     * Llega un dato del semáforo por UDP
     * @param data
     */
    @Override
    public void onDataRead(String data) {
        Log.i("#FSEM# SERVICE", "READ: " + data);

        if (data == null) {
            data = "";
        }

        data = data.substring(data.indexOf('<') + 1, data.indexOf('>'));

        // Pregunto por el largo del mensaje
        if (data.length() != 3) { // Si mide 3 chars es un tiempo.
            try {
                //data = "calle 1;V;20-calle 2;R;20";


                String[] calle1 = data.split("-")[0].split(";");
                String[] calle2 = data.split("-")[1].split(";");

                if ("R".equals(calle1[1])) {
                    this.calleVerde = calle2[0];
                    this.tiempoVerde = Integer.parseInt(calle2[2]);

                    this.calleRojo = calle1[0];
                    this.tiempoRojo = Integer.parseInt(calle1[2]);

                    this.reproducirCompleto = !canCross;
                    this.canCross = true;
                } else {
                    this.calleVerde = calle1[0];
                    this.tiempoVerde = Integer.parseInt(calle1[2]);

                    this.calleRojo = calle2[0];
                    this.tiempoRojo = Integer.parseInt(calle2[2]);

                    this.reproducirCompleto = canCross;
                    this.canCross = false;
                }

                Log.i("#FSEM#", "Cruzando " + calle1[0]);

                continueAskingTimes = true;

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            // Cargo el tiempo que lleva prendida esa luz
            int tiempo = Integer.parseInt(data);

            // Si puede cruzar, la luz prendida es la roja, entonces
            if(canCross){
                // El tiempo disponible es TIEMPO ROJO - TIEMPO DE LUZ ROJA PRENDIDA
                tiempo = tiempoRojo - tiempo;
            } else {
                // De lo contrario, no puede cruzar
                // El tiempo de espera es TIEMPO VERDE - TIEMPO DE LUZ VERDE PRENDIDA
                tiempo = tiempoVerde - tiempo;
            }

            // Solicito para saber siempre el estado de las luces.
            //requestFull();

            // Solo lo uso si no es igual al ultimo tiempo usado, para no repetir segundos.
            if (tiempo != tiempoDisponible && tiempo>0) {
                tiempoDisponible = tiempo;

                // Si es la primera vez que se conecta, reproduce sin importar lo que llegue
                if (this.reproducirCompleto) {
                    if (this.canCross) {
                        textToSpeech.speak("Tiene " + tiempo + " segundos para cruzar " + calleRojo,
                                TextToSpeech.QUEUE_FLUSH, null);
                    } else {
                        textToSpeech.speak("No cruzar. Espere " + tiempo + " segundos.",
                                TextToSpeech.QUEUE_FLUSH, null);
                    }
                    lastAlert = tiempo;
                    this.reproducirCompleto = false;
                } else {
                    if (tiempoDisponible > TIEMPO_AVISO + 1) {
                        // Si es multiplo de 5, o pasaron mas de 5 despues de la ultima alerta
                        if (tiempoDisponible % 5 == 0 || lastAlert - tiempo > 5) {
                            if (this.canCross) {
                                textToSpeech.speak("Tiene " + tiempo + " segundos para cruzar " + calleRojo,
                                        TextToSpeech.QUEUE_FLUSH, null);
                            } else {
                                textToSpeech.speak("Espere " + tiempo + " segundos.",
                                        TextToSpeech.QUEUE_FLUSH, null);
                            }

                            lastAlert = tiempo;
                        }
                    } else {
                        if (tiempoDisponible == TIEMPO_AVISO + 1 && this.canCross) {
                            textToSpeech.speak("Últimos " + TIEMPO_AVISO + " segundos ",
                                    TextToSpeech.QUEUE_FLUSH, null);
                            // Se maneja internamente, sabiendo que faltan 5
                            continueAskingTimes = false;
                            SystemClock.sleep(2000);
                            tiempoDisponible -= 2;
                            while (tiempoDisponible > 1) {
                                textToSpeech.speak("" + tiempoDisponible, TextToSpeech.QUEUE_FLUSH, null);
                                SystemClock.sleep(1000);
                                tiempoDisponible--;
                            }
                            ;

                            textToSpeech.speak("Espere para cruzar.",
                                    TextToSpeech.QUEUE_ADD, null);
                            SystemClock.sleep(2000);
                            invertStreets();
                            requestFull();
                        }
                        if (tiempoDisponible <= 3 && !this.canCross) {
                            textToSpeech.speak("En breve podrá cruzar.",
                                    TextToSpeech.QUEUE_ADD, null);
                            canCross = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * Invierte los roles de las calles, para cambiar el cruce-
     * Es un paleativo en caso de que no llegue respuesa de requestFull.
     */
    private void invertStreets() {
        this.canCross = !this.canCross;
        String calleAux = this.calleVerde;
        this.calleVerde = this.calleRojo;
        this.calleRojo = this.calleVerde;
    }

    public static void staticDisconect() {
        SemComunication.staticState = StaticState.NO_CONECTADO;
        SemComunication.currentSSID = null;
        SemComunication.networkId = -1;
    }
    public static void desconectar() {
        SemComunication.conexion = StaticState.NO_CONECTADO;

    }

    // Enumeracion para estados de conexion
    public enum StaticState {
        CONECTADO, NO_CONECTADO
    }

}