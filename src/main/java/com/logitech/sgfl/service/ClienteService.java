package com.logitech.sgfl.service;

import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.me.Cliente;
import com.logitech.sgfl.repository.ClienteRepository;
import com.logitech.sgfl.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public ClienteService(
            ClienteRepository clienteRepository,
            PedidoRepository pedidoRepository
    ) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public Cliente criar(
            String nome,
            String cpf,
            String email,
            String telefone,
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf
    ) {

        String cpfNormalizado =
                normalizarCpf(cpf);

        String emailNormalizado =
                normalizarEmail(email);

        validarCpf(cpfNormalizado);

        if (clienteRepository.existsByCpf(
                cpfNormalizado
        )) {
            throw new RegraNegocioException(
                    "Já existe um cliente com este CPF."
            );
        }

        if (clienteRepository.existsByEmail(
                emailNormalizado
        )) {
            throw new RegraNegocioException(
                    "Já existe um cliente com este e-mail."
            );
        }

        Cliente cliente =
                new Cliente(
                        nome.trim(),
                        cpfNormalizado,
                        emailNormalizado
                );

        preencher(
                cliente,
                telefone,
                cep,
                logradouro,
                numero,
                complemento,
                bairro,
                cidade,
                uf
        );

        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente atualizar(
            Long id,
            String nome,
            String cpf,
            String email,
            String telefone,
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf,
            boolean ativo
    ) {

        Cliente cliente =
                buscar(id);

        String cpfNormalizado =
                normalizarCpf(cpf);

        String emailNormalizado =
                normalizarEmail(email);

        validarCpf(cpfNormalizado);

        if (clienteRepository.existsByCpfAndIdNot(
                cpfNormalizado,
                id
        )) {
            throw new RegraNegocioException(
                    "Já existe outro cliente com este CPF."
            );
        }

        if (clienteRepository.existsByEmailAndIdNot(
                emailNormalizado,
                id
        )) {
            throw new RegraNegocioException(
                    "Já existe outro cliente com este e-mail."
            );
        }

        cliente.setNome(nome.trim());
        cliente.setCpf(cpfNormalizado);
        cliente.setEmail(emailNormalizado);
        cliente.setAtivo(ativo);

        preencher(
                cliente,
                telefone,
                cep,
                logradouro,
                numero,
                complemento,
                bairro,
                cidade,
                uf
        );

        return clienteRepository.save(cliente);
    }

    @Transactional
    public void excluir(Long id) {

        Cliente cliente =
                buscar(id);

        if (pedidoRepository.existsByCliente_Id(id)) {
            throw new RegraNegocioException(
                    "Não é possível excluir o cliente porque ele possui pedidos cadastrados. " +
                            "Desative o cliente em vez de apagar seu histórico."
            );
        }

        clienteRepository.delete(cliente);
    }

    @Transactional
    public Cliente buscar(Long id) {

        return clienteRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Cliente não encontrado: " + id
                        )
                );
    }

    @Transactional
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Transactional
    public Page<Cliente> listar(Pageable pageable) {
        return clienteRepository.findAll(pageable);
    }

    private void preencher(
            Cliente cliente,
            String telefone,
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf
    ) {

        cliente.setTelefone(
                normalizarTexto(telefone)
        );

        cliente.setCep(
                normalizarTexto(cep)
        );

        cliente.setLogradouro(
                normalizarTexto(logradouro)
        );

        cliente.setNumero(
                normalizarTexto(numero)
        );

        cliente.setComplemento(
                normalizarTexto(complemento)
        );

        cliente.setBairro(
                normalizarTexto(bairro)
        );

        cliente.setCidade(
                normalizarTexto(cidade)
        );

        cliente.setUf(
                normalizarUf(uf)
        );
    }

    private String normalizarCpf(String cpf) {

        if (cpf == null) {
            return "";
        }

        return cpf.replaceAll(
                "\\D",
                ""
        );
    }

    private void validarCpf(String cpf) {

        if (cpf.length() != 11) {

            throw new RegraNegocioException(
                    "O CPF deve conter 11 dígitos."
            );
        }

        if (cpf.chars().distinct().count() == 1) {

            throw new RegraNegocioException(
                    "CPF inválido."
            );
        }
    }

    private String normalizarEmail(String email) {

        return email == null
                ? ""
                : email.trim().toLowerCase();
    }

    private String normalizarTexto(String texto) {

        if (texto == null) {
            return null;
        }

        String resultado =
                texto.trim();

        return resultado.isEmpty()
                ? null
                : resultado;
    }

    private String normalizarUf(String uf) {

        String resultado =
                normalizarTexto(uf);

        return resultado == null
                ? null
                : resultado.toUpperCase();
    }
}
