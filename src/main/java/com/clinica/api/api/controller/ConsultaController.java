package com.clinica.api.api.controller;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clinica.api.domain.exception.RecursoNaoEncontradaException;
import com.clinica.api.domain.model.Consulta;
import com.clinica.api.domain.repository.IConsultaRepository;
import com.clinica.api.domain.service.ConsultaService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping(value = "/consultas")
public class ConsultaController {

	@Autowired
	private IConsultaRepository iConsultaRepository;
	
	@Autowired
	private ConsultaService consultaService;
	
	@GetMapping
	public List<Consulta> listar(
		
			@RequestParam(name = "idConsulta",       required = false)   Long idConsulta,
			@RequestParam(name = "descricao", 	     required = false)  String descricao, 
			@RequestParam(name = "valorConsulta",    required = false) @NumberFormat(pattern = "#0,00") BigDecimal valorConsulta, 
			@RequestParam(name = "idMedico", 	     required = false)  Long idMedico			
			
			) {
		return consultaService.listarConsultas(idConsulta, descricao, valorConsulta, idMedico);
	}
	
	@GetMapping("/{consultaId}")
	public ResponseEntity<Consulta> buscar(@PathVariable Long consultaId) {
		Optional<Consulta> consulta = iConsultaRepository.findById(consultaId);
		
		if (consulta.isPresent()) {
			return ResponseEntity.ok(consulta.get());
		}
		
		return ResponseEntity.notFound().build();
	}
	
	@PostMapping
	public ResponseEntity<?> adicionar(@RequestBody Consulta consulta) {
		try {
			consulta = consultaService.salvar(consulta);
			
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(consulta);
		} catch (RecursoNaoEncontradaException e) {
			return ResponseEntity.badRequest()
					.body(e.getMessage());
		}
	}
	/**
	 * @param consultaId recebe o código a ser atualizado
	 * @param consulta
	 * @return os dados da consulta médica atualizada
	 * 
	 * os nomes de edpoint deve ser alterado e padronizado conforme sua necessidade
	 */
	@PutMapping("/{consultaId}")
	public ResponseEntity<?> atualizar(@PathVariable Long consultaId,
			@RequestBody Consulta consulta) {
		try {
			Consulta consultaMedicaAtual = iConsultaRepository.findById(consultaId).orElse(null);
			
			if (consultaMedicaAtual != null) {
				BeanUtils.copyProperties(consulta, consultaMedicaAtual, "id");
				
				consultaMedicaAtual = consultaService.salvar(consultaMedicaAtual);
				return ResponseEntity.ok(consultaMedicaAtual);
			}
			
			return ResponseEntity.notFound().build();
		
		} catch (RecursoNaoEncontradaException e) {
			return ResponseEntity.badRequest()
					.body(e.getMessage());
		}
	}
	/**
	 * 
	 * @param valorInicial recebe um valor inicial para realizar o get
	 * @param valorFinal  recebe um valor final para realizar o get
	 * @return uma lista de consultas por intervalo de valor
	 * exemplo:http://localhost:8080/consultas/por-valor-consulta?valorInicial=1&valorFinal=500
	 * 
	 * os nomes de edpoint deve ser alterado e padronizado conforme sua necessidade
	 */
	
	@GetMapping("/por-valor-consulta")
	public List<Consulta> consultasPorValorInicialFinal(
			BigDecimal valorInicial, BigDecimal valorFinal) {
		return iConsultaRepository.findByValorConsultaBetween(valorInicial, valorFinal);
	}
	/**
	 * 
	 * @param descricao recebe a descrição da consulta medica
	 * @param medicoId recebe o id do medico cadastrado
	 * @return retorna uma lista de consultas relacionado a um determinado id do medico
	 * 
	 * exemplo teste realizado no postman: http://localhost:8080/consultas/por-descricao-consulta-id?descricao=paciente&medicoId=1
	 * 
	 * os nomes de edpoint deve ser alterado e padronizado conforme sua necessidade
	 */
	@GetMapping("/por-descricao-consulta-id")
	public List<Consulta> consultasPorNomeId(
			String descricao, Long medicoId) {
		return iConsultaRepository.findByDescricaoContainingAndMedicoId(descricao, medicoId);
	}
	
	/**
	 * 
	 * @param descricao recebe a descrição da consulta medica
	 * @param medicoId recebe o id do medico cadastrado
	 * @return retorna uma lista de consultas relacionado a um determinado id do medico
	 * 
	 * exemplo teste realizado no postman: http://localhost:8080/consultas/por-descricao-consulta-id?descricao=paciente&medicoId=1
	 * 
	 * os nomes de edpoint deve ser alterado e padronizado conforme sua necessidade
	 * este é exemplo foi customizado utilizando @Query + JPQL
	 */
	@GetMapping("/por-descricao-consulta-idmedico")
	public List<Consulta> descricaoConsultaIdMedicosCadastrado( String descricao, Long medicoId) {
		return iConsultaRepository.descricaoConsultaIdMedico(descricao, medicoId);
	}
	
	/** 
	 * consultas utilizando Query em arquivo xml customizado
	 * 
	 * @param descricao recebe a descrição da consulta medica
	 * @param medicoId recebe o id do medico cadastrado
	 * @return  retorna uma lista de consultas relacionado a um determinado id do medico 
	 */
	@GetMapping("/descricao-consulta-id-relacionada")
	public List<Consulta> descricaoConsultaIdMedicoRelacionado(
			String descricao, Long medicoId) {
		return iConsultaRepository.descricaoConsultaIdMedicoRelacionado(descricao, medicoId);
	}
	

	@PatchMapping("/{consultaId}")
	public ResponseEntity<?> atualizarParcial(@PathVariable Long consultaId,
			@RequestBody Map<String, Object> campos) {
		Consulta consultaAtual = iConsultaRepository.findById(consultaId).orElse(null);
		
		if (consultaAtual == null) {
			return ResponseEntity.notFound().build();
		}
		
		merge(campos, consultaAtual);
		
		return atualizar(consultaId, consultaAtual);
	}
	
	
	

	private void merge(Map<String, Object> dadosOrigem, Consulta consultaDestino) {
		ObjectMapper objectMapper = new ObjectMapper();
		Consulta consultaOrigem = objectMapper.convertValue(dadosOrigem, Consulta.class);
		
		dadosOrigem.forEach((nomePropriedade, valorPropriedade) -> {
			Field field = ReflectionUtils.findField(Consulta.class, nomePropriedade);
			field.setAccessible(true);
			
			Object novoValor = ReflectionUtils.getField(field, consultaOrigem);
			
			ReflectionUtils.setField(field, consultaDestino, novoValor);
		});
	}
	
	
	
	
}
