import com.fazecast.jSerialComm.*;
import java.io.*;
import java.net.*;

public class Rasberry_Client {
    public static void main(String[] args) throws IOException {
        // Seri portu seç ve başlat
        SerialPort selectedPort = selectAndOpenSerialPort();
        if (selectedPort == null) {
            System.err.println("Seri port seçimi başarısız.");
            return;
        }

        // Sunucuya bağlan
        Socket socket = connectToServer();
        if (socket == null) {
            System.err.println("Sunucuya bağlanılamadı.");
            selectedPort.closePort();
            return;
        }

        // Giriş ve çıkış akışlarını oluştur
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Seri porttan veri okumak için bir dinleyici oluştur
        selectedPort.addDataListener(new SerialPortDataListener() {
            StringBuilder buffer = new StringBuilder();

            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return;
                byte[] newData = new byte[selectedPort.bytesAvailable()];
                int numRead = selectedPort.readBytes(newData, newData.length);
                String receivedData = new String(newData);

                // Veriyi tampona ekle
                buffer.append(receivedData);

                // Tamponu kontrol et ve sunucuya gönder
                if (buffer.indexOf("\n") != -1) {
                    String dataToSend = buffer.toString().trim();
                    out.println(dataToSend);
                    System.out.println(" - Sunucuya gönderildi: " + dataToSend);
                    buffer.setLength(0); // Tamponu temizle
                }
            }
        });

        // Programın sonlanmasını engellemek için beklet
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static SerialPort selectAndOpenSerialPort() {
        // Seri portları listele
        SerialPort[] ports = SerialPort.getCommPorts();
        SerialPort selectedPort = null;
        System.out.println("Bulunan seri portlar:");

        for (SerialPort port : ports) {
            System.out.println(port.getDescriptivePortName());
            if (port.getDescriptivePortName().toLowerCase().contains("stmicroelectronics stlink virtual com port")) {
                selectedPort = port;
                break; // STMicroelectronics portunu bulduğumuzda döngüyü sonlandır
            }
        }

        if (selectedPort == null) {
            System.err.println("STMicroelectronics USB portu bulunamadı.");
            return null;
        }

        System.out.println("STMicroelectronics USB portu bulundu: " + selectedPort.getSystemPortName());

        if (selectedPort.openPort()) {
            System.out.println("Port başarıyla açıldı.");
            return selectedPort;
        } else {
            System.err.println("Port açılamadı.");
            return null;
        }
    }

    private static Socket connectToServer() {
        try {
            Socket socket = new Socket("172.15.1.16", 3131);
            System.out.println("Sunucuya bağlanıldı.");
            return socket;
        } catch (Exception e) {
            System.err.println("Sunucuya bağlanılamadı: " + e.getMessage());
            return null;
        }
    }
}
