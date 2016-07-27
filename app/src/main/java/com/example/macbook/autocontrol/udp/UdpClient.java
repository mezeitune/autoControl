package com.example.macbook.autocontrol.udp;

import android.util.Log;

import com.example.macbook.autocontrol.service.SemComunication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class UdpClient {

    private DatagramSocket socket;
    private ClientThread clientThread;
    private String IP;
    private int port;
    private UdpDataHandler handler = null;
    private InetAddress serverAddr = null;

    // Buffer de envio
    byte[] sendData = new byte[1024];
    // Buffer de recepcion
    byte[] receiveData = new byte[1024];

    public UdpClient(String paramIP, int paramPort, UdpDataHandler paramHandler) {

        //this.clientThread = new ClientThread(this);
        this.IP = paramIP;
        this.port = paramPort;
        this.handler = paramHandler;

    }

    /**
     * Constructor sin handler
     */
    public UdpClient(String paramIP, int paramPort) {
        this(paramIP, paramPort, null);
    }

    /**
     * Conecta al socket de forma paralela
     */
    public boolean connect() {
        return this.connect(true);
    }

    /**
     * Conecta al socket de forma paralela o lineal
     */
    public boolean connect(boolean newThread) {

        if (newThread) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        serverAddr = InetAddress.getByName(IP);
                        socket = connectSocket(serverAddr, port);
                        if (socket != null /* && socket.isConnected() */) {
                            handler.onConected(true);

                            while (socket != null /* && socket.isConnected() */) {
                                String data = read();
                                if (data != null) {
                                    handler.onDataRead(data);
                                }
                            }
                        } else {
                            handler.onConected(false);
                        }
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
            return true;
        } else {
            socket = connectSocket(serverAddr, port);
            return socket != null;
        }


    }

    /**
     * Crea un socket para la ip y el puerto deseados
     */
    public static DatagramSocket connectSocket(InetAddress serverAddr, int port) {

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
            //socket.connect(serverAddr, port);

        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return socket;
    }

    /**
     * Envio "data" por UDP
     *
     * @param data
     */
    public void send(final String data) {
        Log.i("#FSEM# MANDO TCP", data);
        if (socket != null) {
            // Envío el dato en un nuevo hilo para evitar interrumpir el hilo principal.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendData = data.getBytes("US-ASCII");
                        DatagramPacket sendPacket = new DatagramPacket(sendData, data.length(), serverAddr, port);
                        socket.send(sendPacket);
                    } catch (UnknownHostException e) {
                        Log.i("--1----------------", "-");
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.i("--error2--------------", "-");
                   //   socket.disconnect(); // RECONECTA PERO LO DICE 2/3/4/5/N VECES LO MISMO
                      //(según las veces que reconecto)

                      //socket.close(); //SALE SIEMPRE POR ERROR 5

                      //socket= null; //SE RECONECTA PERO  NO REPRODUCE AUDIO a veces estando conectado

                       SemComunication.staticDisconect();
                        SemComunication.client.close();
                        //Hace lo mismo que socket.disconnect(); ,
                        // se reconecta, habla pero por cada desconexión no cierra el socket anterior y después
                        // cuando reconecta dice las cosas 2 veces seguidas, despues si reconecta de nuevo
                        //las dice 3 veces seguidas y así...


                        //SemComunication.wifi.disconnect(); //Esto o finalizar el hilo que se inicia
                        // en el create de SemComunication sería lo ideal, se puede??


                       // e.printStackTrace();
                    } catch (Exception e) {
                        Log.i("--3-----------------", "-");
                        e.printStackTrace();
                    }
                }
            }).start();

        } else {
            Log.e("#FSEM# TCP", "NULL SOCKET");
        }
    }

    public String read() {
        try {
            //SE DESCOMENTO
            if (socket != null  /*&& socket.isConnected()*/ ) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length, serverAddr, port);
                socket.receive(receivePacket);
                String data = new String(receivePacket.getData());
                if (data != null) {
                    Log.i("#FSEM# LEO TCP", data);
                }
                return data;
            } else {
                Log.e("#FSEM# TCP", "NULL SOCKET");
            }
        } catch (UnknownHostException e) {
            Log.i("--4-----------------", "-");
            e.printStackTrace();
        } catch (IOException e) {
            Log.i("--5---------------", "-");

           // e.printStackTrace();
            // Si entra aca probablemente buscaba leer un dato y el socket fue cerrado.
        } catch (Exception e) {
            Log.i("--6---------------", "-");
            e.printStackTrace();
        }
        // Si se cerro el socket devuelvo un dato nulo
        return null;
    }

    public void close() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
            } else {
                Log.e("#FSEM# TCP", "NULL SOCKET");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return this.socket != null; //  && this.socket.isConnected();
    }

    class ClientThread implements Runnable {

        private UdpClient parent;

        public ClientThread(UdpClient parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(this.parent.IP);
                this.parent.socket = new DatagramSocket(this.parent.port, serverAddr);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

}