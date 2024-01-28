package com.m01libDEN;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final long timeWindow;
    private final AtomicInteger counter;
    private final Lock lock = new ReentrantLock();
    private long firstRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        if (requestLimit > 0) {
            this.requestLimit = requestLimit;
            counter = new AtomicInteger(requestLimit);
        } else {
            throw new IllegalArgumentException("Передано некорректное значение requestLimit");
        }
        timeWindow = TimeUnit.MILLISECONDS.convert(1L, timeUnit);
    }

    public boolean isLimit() {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (counter.get() == requestLimit) {
                firstRequestTime = System.currentTimeMillis();
                counter.decrementAndGet();
            } else if (counter.get() > 0 && currentTime <= (firstRequestTime + timeWindow)) {
                counter.decrementAndGet();
            } else if (counter.get() > 0 && currentTime > (firstRequestTime + timeWindow)) {
                firstRequestTime = System.currentTimeMillis();
                counter.set(requestLimit - 1);
            } else if (counter.get() == 0 && currentTime <= (firstRequestTime + timeWindow)) {
                try {
                    Thread.sleep(firstRequestTime + timeWindow - currentTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                firstRequestTime = System.currentTimeMillis();
                counter.set(requestLimit - 1);
            } else if (counter.get() == 0 && currentTime > (firstRequestTime + timeWindow)) {
                firstRequestTime = System.currentTimeMillis();
                counter.set(requestLimit - 1);
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    public void postRequest(CrptDocument document) {
        if (!isLimit()) {
            String requestString = documentToJSON(document).toString();
            try {
                final Content postResult = Request.Post(URL)
                        .bodyString(requestString, ContentType.APPLICATION_JSON)
                        .execute().returnContent();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private JSONObject documentToJSON(CrptDocument document) {
        JSONObject docJSON = new JSONObject();
        if (document.getDescription() != null) {
            JSONObject inn = new JSONObject();
            inn.put("participantInn", document.getParticipantInn());
            docJSON.put("description", inn);
        }
        docJSON.put("doc_id", document.getDocId());
        docJSON.put("doc_status", document.getDocStatus());
        docJSON.put("doc_type", document.getDocType());
        if (document.getImportRequest() != null) {
            docJSON.put("importRequest", document.getImportRequest());
        }
        docJSON.put("owner_inn", document.getOwnerInn());
        docJSON.put("participant_inn", document.getParticipantInn());
        docJSON.put("producer_inn", document.getProducerInn());
        docJSON.put("production_date", document.getProducerInn());
        docJSON.put("production_type", document.getProductionType());
        Product[] products = document.getProducts();
        if (products != null) {
            JSONArray productsList = new JSONArray();
            for (Product product : products) {
                JSONObject productJSON = new JSONObject();
                if (product.getCertificateDocument() != null) {
                    productJSON.put("certificate_document", product.getCertificateDocument());
                } else if (product.getCertificateDocumentDate() != null) {
                    productJSON.put("certificate_document_date", product.getCertificateDocumentDate());
                } else if (product.getCertificateDocumentNumber() != null) {
                    productJSON.put("certificate_document_number", product.getCertificateDocumentNumber());
                }
                productJSON.put("owner_inn", document.getOwnerInn());
                productJSON.put("producer_inn", document.getProducerInn());
                productJSON.put("production_date", document.getProductionDate());
                if (!document.getProductionDate().equals(product.getProductionDate())) {
                    productJSON.put("production_date", product.getProductionDate());
                }
                productJSON.put("tnved_code", product.getTnvedCode());
                if (product.getUitCode() != null) {
                    productJSON.put("uit_code", product.getUitCode());
                } else if (product.getUituCode() != null) {
                    productJSON.put("uitu_code", product.getUituCode());
                } else {
                    throw new IllegalArgumentException("Одно из полей uit_code/uitu_code " +
                            "является обязательным");
                }
                productsList.add(productJSON);
            }
            docJSON.put("products", productsList);
        }
        docJSON.put("reg_date", document.getRegDate());
        docJSON.put("reg_number", document.getRegNumber());
        return docJSON;
    }

    public class CrptDocument {
        private String description;
        private final String participantInn;
        private final String docId;
        private final String docStatus;
        private final String docType;
        private String importRequest;
        private final String ownerInn;
        private final String producerInn;
        private final String productionDate;
        private final String productionType;
        private final String regDate;
        private final String regNumber;
        private Product[] products;

        public CrptDocument(String description, String participantInn, String docId,
                            String docStatus, String docType, String importRequest,
                            String ownerInn, String producerInn, String productionDate,
                            String productionType, String regDate, String regNumber,
                            Product[] products) {
            this.description = description;
            this.participantInn = participantInn;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.regDate = regDate;
            this.regNumber = regNumber;
            this.products = products;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public String getDocId() {
            return docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public String getImportRequest() {
            return importRequest;
        }

        public void setImportRequest(String importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public String getRegDate() {
            return regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public Product[] getProducts() {
            return products;
        }

        public void setProducts(Product[] products) {
            this.products = products;
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

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }

        public String getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public void setCertificateDocumentDate(String certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
        }

        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public void setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public String getTnvedCode() {
            return tnvedCode;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public String getUitCode() {
            return uitCode;
        }

        public void setUitCode(String uitCode) {
            this.uitCode = uitCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }
    }
}
