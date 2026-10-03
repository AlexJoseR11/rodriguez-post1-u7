package com.example.multas.repository;

import com.example.multas.model.EstadoMulta;
import com.example.multas.model.Multa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MultaRepository extends JpaRepository<Multa, Long> {

    /**
     * Consulta todas las multas asociadas a un estudiante específico.
     *
     * @param estudianteId Identificador del estudiante.
     * @return Lista de multas encontradas.
     */
    List<Multa> findByEstudianteId(String estudianteId);

    /**
     * Cuenta el número de multas de un estudiante filtradas por su estado.
     * Delega el conteo eficientemente al motor SQL (COUNT(*)) con complejidad de transferencia O(1),
     * evitando la sobrecarga de cargar todas las entidades en la memoria de la JVM.
     *
     * @param estudianteId Identificador del estudiante.
     * @param estado Estado de la multa (e.g. PENDIENTE, PAGADA).
     * @return Cantidad de multas registradas en dicho estado.
     */
    long countByEstudianteIdAndEstado(String estudianteId, EstadoMulta estado);
}
