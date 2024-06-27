import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
//Внешняя библиотека для json
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class CrptApi {

    private final HttpClient httpClient;
    private final Semaphore semaphore;
    private final ReentrantLock lock;
    private final Gson gson;
    private final String apiUrl;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this(timeUnit, requestLimit, HttpClient.newHttpClient(), new Gson(), "https://ismp.crpt.ru/api/v3/lk/documents/create");
    }

    //Позволяет использовать dependency injection для gson и httpClient 
    public CrptApi(TimeUnit timeUnit, int requestLimit, HttpClient httpClient, Gson gson, String apiUrl) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.apiUrl = apiUrl;
        this.lock = new ReentrantLock();

        int permits = requestLimit;
        long period = timeUnit.toMillis(1);

        this.semaphore = new Semaphore(permits);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            //реализуем thread-safe
            lock.lock();
            try {
                semaphore.release(permits - semaphore.availablePermits());
            } finally {
                lock.unlock();
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, ExecutionException, TimeoutException {
        semaphore.acquire();
        lock.lock();
        try {
            JsonObject jsonDocument = gson.toJsonTree(document).getAsJsonObject();
            jsonDocument.addProperty("signature", signature);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(jsonDocument)))
                    .build();

            CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            // TODO: Вывод результата, возможно, нужен в другой форме, а так только печатается в консоли
            System.out.println(response.get().body());

        } finally {
            lock.unlock();
            semaphore.release();
        }
    }

    public static class Document {
        private String participantInn;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private Product[] products;
        private String regDate;
        private String regNumber;

        // Сеттеры
        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public void setDocStatus(String docStatus) {
            this.docStatus = docStatus;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public void setProductionType(String productionType) {
            this.productionType = productionType;
        }

        public void setProducts(Product[] products) {
            this.products = products;
        }

        public void setRegDate(String regDate) {
            this.regDate = regDate;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }
    }

    public static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;

        // Сеттеры
        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }

        public void setCertificateDocumentDate(String certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
        }

        public void setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public void setUitCode(String uitCode) {
            this.uitCode = uitCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }
    }
}

//Использование класса CrptApi

public class CrptApiExample {

    public static void main(String[] args) {
        // 5 запросов в минуту
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 3);

        CrptApi.Product product = new CrptApi.Product();
        product.setCertificateDocument("certDoc");
        product.setCertificateDocumentDate("2020-01-23");
        product.setCertificateDocumentNumber("certNumber");
        product.setOwnerInn("1234567890");
        product.setProducerInn("1234567890");
        product.setProductionDate("2020-01-23");
        product.setTnvedCode("code");
        product.setUitCode("uitCode");
        product.setUituCode("uituCode");

        CrptApi.Document document = new CrptApi.Document();
        document.setParticipantInn("1234567890");
        document.setDocId("doc123");
        document.setDocStatus("NEW");
        document.setDocType("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwnerInn("1234567890");
        document.setProducerInn("1234567890");
        document.setProductionDate("2020-01-23");
        document.setProductionType("Type1");
        document.setRegDate("2020-01-23");
        document.setRegNumber("reg123");

        // Добавляем продукт в документ
        document.setProducts(new CrptApi.Product[]{product});


        // Подпись документа
        String signature = "signature";

        // Отправляем документ в API
        try {
            crptApi.createDocument(document, signature);
            System.out.println("Документ успешно отправлен!");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Ошибка при отправке документа: " + e.getMessage());
        }
    }
}

//Тестовые данные
// {
//     "description": {
//       "participantInn": "string"
//     },
//     "doc_id": "string",
//     "doc_status": "string",
//     "doc_type": "LP_INTRODUCE_GOODS",
//     "importRequest": true,
//     "owner_inn": "string",
//     "participant_inn": "string",
//     "producer_inn": "string",
//     "production_date": "2020-01-23",
//     "production_type": "string",
//     "products": [
//       {
//         "certificate_document": "string",
//         "certificate_document_date": "2020-01-23",
//         "certificate_document_number": "string",
//         "owner_inn": "string",
//         "producer_inn": "string",
//         "production_date": "2020-01-23",
//         "tnved_code": "string",
//         "uit_code": "string",
//         "uitu_code": "string"
//       }
//     ],
//     "reg_date": "2020-01-23",
//     "reg_number": "string"
// }