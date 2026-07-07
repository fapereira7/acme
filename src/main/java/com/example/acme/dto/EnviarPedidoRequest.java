package com.example.acme.dto;

public record EnviarPedidoRequest(
        String numPedido,
        String cantidadPedido,
        String codigoEAN,
        String nombreProducto,
        String numDocumento,
        String direccion
) {}