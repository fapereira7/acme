package com.example.acme.controller;

import com.example.acme.dto.EnviarPedidoRequest;
import com.example.acme.dto.EnviarPedidoResponse;
import com.example.acme.dto.RestPedidoWrapper;
import com.example.acme.dto.RestResponseWrapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/v1")
public class PedidoController {
    private final RestClient restClient;

    // Forzamos el puerto 8081 que es donde Docker Compose expone a WireMock
    public PedidoController() {
        this.restClient = RestClient.create("http://localhost:8081");
    }

    @PostMapping(value = "/pedidos", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseWrapper procesarPedido(@RequestBody RestPedidoWrapper requestWrapper) {
        EnviarPedidoRequest pedido = requestWrapper.enviarPedido();

        // 1. Tu método de transformación existente
        String soapRequestXml = transformarJsonToXml(pedido);

        // 2. Envío forzado con URL absoluta completa hacia WireMock
        String soapResponseXml = restClient.post()
                .uri("http://localhost:8081/api/v1/mock-soap-endpoint") // <-- URL absoluta completa aquí
                .contentType(MediaType.TEXT_XML)
                .body(soapRequestXml)
                .retrieve()
                .body(String.class);

        // 3. Tu método de procesamiento de respuesta existente
        EnviarPedidoResponse response = transformarXmlToJson(soapResponseXml);
        return new RestResponseWrapper(response);
    }

    @PostMapping(value = "/mock-soap-endpoint", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
    public String mockSoapEndpoint(@RequestBody String requestXml) {
        return """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:env="http://WSDLs/EnvioPedidos/EnvioPedidosAcme">
               <soapenv:Header/>
               <soapenv:Body>
                  <env:EnvioPedidoAcmeResponse>
                     <EnvioPedidoResponse>
                        <Codigo>80375472</Codigo>
                        <Mensaje>Entregado exitosamente al cliente</Mensaje>
                     </EnvioPedidoResponse>
                  </env:EnvioPedidoAcmeResponse>
               </soapenv:Body>
            </soapenv:Envelope>
            """;
    }

    // --- Métodos de Transformación ---

    private String transformarJsonToXml(EnviarPedidoRequest p) {
        return """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:env="http://WSDLs/EnvioPedidos/EnvioPedidosAcme">
               <soapenv:Header/>
               <soapenv:Body>
                  <env:EnvioPedidoAcme>
                     <EnvioPedidoRequest>
                        <pedido>%s</pedido>
                        <Cantidad>%s</Cantidad>
                        <EAN>%s</EAN>
                        <Producto>%s</Producto>
                        <Cedula>%s</Cedula>
                        <Direccion>%s</Direccion>
                     </EnvioPedidoRequest>
                  </env:EnvioPedidoAcme>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(p.numPedido(), p.cantidadPedido(), p.codigoEAN(), p.nombreProducto(), p.numDocumento(), p.direccion());
    }

    private EnviarPedidoResponse transformarXmlToJson(String xml) {
        String codigo = extraerTag(xml, "Codigo");
        String mensaje = extraerTag(xml, "Mensaje");

        return new EnviarPedidoResponse(codigo, mensaje);
    }

    private String extraerTag(String xml, String tag) {
        int inicio = xml.indexOf("<" + tag + ">") + tag.length() + 2;
        int fin = xml.indexOf("</" + tag + ">");
        if (inicio < 0 || fin < 0) return "";
        return xml.substring(inicio, fin);
    }
}
