package com.example.multas.controller;

import com.example.multas.model.Multa;
import com.example.multas.service.MultaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/multas")
public class MultaController {

    private final MultaService multaService;

    public MultaController(MultaService multaService) {
        this.multaService = multaService;
    }

    @GetMapping
    public ResponseEntity<List<Multa>> listarTodas() {
        return ResponseEntity.ok(multaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Multa> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(multaService.buscarPorId(id));
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<Multa>> listarPorEstudiante(@PathVariable String estudianteId) {
        return ResponseEntity.ok(multaService.listarPorEstudiante(estudianteId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Multa> generar(@Valid @RequestBody GenerarMultaRequest request) {
        Multa multa = multaService.generar(
                request.estudianteId(),
                request.concepto(),
                request.diasAtraso()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(multa);
    }

    @PatchMapping("/{id}/pagar")
    public ResponseEntity<Multa> pagarEnVentanilla(@PathVariable Long id) {
        Multa multa = multaService.pagarEnVentanilla(id);
        return ResponseEntity.ok(multa);
    }

    @PostMapping("/{id}/pagar-en-linea")
    public ResponseEntity<Multa> pagarEnLinea(@PathVariable Long id) {
        Multa multa = multaService.pagarConPasarela(id);
        return ResponseEntity.ok(multa);
    }
}
