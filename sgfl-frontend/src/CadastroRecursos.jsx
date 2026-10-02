import { useEffect, useRef, useState } from 'react';
import api from './api';
import useTabTransition from './useTabTransition';

export default function CadastroRecursos({
                                             theme,
                                             veiculos,
                                             motoristas,
                                             onClose,
                                             onAtualizar
                                         }) {
    const [fechando, setFechando] = useState(false);
    const fechamentoTimeoutRef = useRef(null);
    const {
        aba,
        trocarAba: setAba,
        faseTransicao,
        transicionando
    } = useTabTransition('motorista');
    const classePainelAba = faseTransicao === 'saindo'
        ? 'sgfl-tab-transition-exit'
        : faseTransicao === 'entrando'
            ? 'sgfl-tab-transition-enter'
            : '';

    const [motoristaEditando, setMotoristaEditando] =
        useState(null);

    const [veiculoEditando, setVeiculoEditando] =
        useState(null);

    const [nome, setNome] = useState('');
    const [cpf, setCpf] = useState('');
    const [tipoCNH, setTipoCNH] = useState('B');

    const [tipoVeiculo, setTipoVeiculo] =
        useState('caminhao');

    const [placa, setPlaca] = useState('');
    const [modelo, setModelo] = useState('');
    const [capacidade, setCapacidade] = useState('');
    const [quantidadeEixos, setQuantidadeEixos] =
        useState('');

    const [volumeM3, setVolumeM3] =
        useState('');

    const [mensagem, setMensagem] =
        useState('');

    const [erro, setErro] =
        useState('');

    const [salvando, setSalvando] =
        useState(false);

    useEffect(() => () => {
        window.clearTimeout(fechamentoTimeoutRef.current);
    }, []);

    const fechar = () => {
        if (salvando || fechando || fechamentoTimeoutRef.current) return;

        setFechando(true);
        const movimentoReduzido = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
        fechamentoTimeoutRef.current = window.setTimeout(() => {
            fechamentoTimeoutRef.current = null;
            onClose();
        }, movimentoReduzido ? 0 : 180);
    };

    const limparMensagens = () => {
        setMensagem('');
        setErro('');
    };

    const resetarFormularioMotorista = () => {
        setMotoristaEditando(null);
        setNome('');
        setCpf('');
        setTipoCNH('B');
    };

    const resetarFormularioVeiculo = () => {
        setVeiculoEditando(null);
        setTipoVeiculo('caminhao');
        setPlaca('');
        setModelo('');
        setCapacidade('');
        setQuantidadeEixos('');
        setVolumeM3('');
    };

    const handleCpfChange = (event) => {

        setCpf(
            event.target.value
                .replace(/\D/g, '')
                .slice(0, 11)
        );
    };

    const handlePlacaChange = (event) => {

        setPlaca(
            event.target.value
                .replace(
                    /[^a-zA-Z0-9]/g,
                    ''
                )
                .toUpperCase()
                .slice(0, 7)
        );
    };

    const tratarErro = (
        error,
        mensagemPadrao
    ) => {

        const status =
            error?.response?.status;

        const mensagemServidor =
            error?.response?.data?.message;

        if (status === 401) {
            setErro(
                'Sua sessão expirou. Faça login novamente.'
            );
            return;
        }

        if (status === 403) {
            setErro(
                'Você não possui permissão para realizar esta operação.'
            );
            return;
        }

        if (status === 404) {
            setErro(
                mensagemServidor ||
                'Registro não encontrado.'
            );
            return;
        }

        if (status === 409) {
            setErro(
                mensagemServidor ||
                'Não foi possível realizar a operação porque existe um conflito.'
            );
            return;
        }

        setErro(
            mensagemServidor ||
            mensagemPadrao
        );
    };

    const iniciarEdicaoMotorista = (
        motorista
    ) => {

        setAba('motorista');
        limparMensagens();

        setMotoristaEditando(
            motorista.id
        );

        setNome(
            motorista.nome || ''
        );

        setCpf(
            motorista.cpf
                ? motorista.cpf
                    .replace(/\D/g, '')
                : ''
        );

        setTipoCNH(
            motorista.tipoCNH || 'B'
        );
    };

    const iniciarEdicaoVeiculo = (
        veiculo
    ) => {

        limparMensagens();

        setAba('veiculo');

        setVeiculoEditando(
            veiculo.id
        );

        const ehCaminhao =
            veiculo.quantidadeEixos !==
            undefined &&
            veiculo.quantidadeEixos !==
            null;

        setTipoVeiculo(
            ehCaminhao
                ? 'caminhao'
                : 'furgao'
        );

        setPlaca(
            veiculo.placa || ''
        );

        setModelo(
            veiculo.modelo || ''
        );

        setCapacidade(
            veiculo.capacidadeCargaKg ??
            ''
        );

        if (ehCaminhao) {
            setQuantidadeEixos(
                veiculo.quantidadeEixos
            );

            setVolumeM3('');
        } else {

            setVolumeM3(
                veiculo.volumeM3 ?? ''
            );

            setQuantidadeEixos('');
        }
    };

    const excluirMotorista = async (
        motorista
    ) => {

        if (
            !window.confirm(
                `Excluir o motorista "${motorista.nome}"?`
            )
        ) {
            return;
        }

        setSalvando(true);
        limparMensagens();

        try {

            await api.delete(
                `/motoristas/${motorista.id}`
            );

            setMensagem(
                'Motorista excluído com sucesso.'
            );

            if (
                motoristaEditando ===
                motorista.id
            ) {
                resetarFormularioMotorista();
            }

            await onAtualizar();

        } catch (error) {

            tratarErro(
                error,
                'Não foi possível excluir o motorista.'
            );

        } finally {

            setSalvando(false);
        }
    };

    const excluirVeiculo = async (
        veiculo
    ) => {

        if (
            !window.confirm(
                `Excluir o veículo "${veiculo.modelo}" - ${veiculo.placa}?`
            )
        ) {
            return;
        }

        setSalvando(true);
        limparMensagens();

        try {

            await api.delete(
                `/veiculos/${veiculo.id}`
            );

            setMensagem(
                'Veículo excluído com sucesso.'
            );

            if (
                veiculoEditando ===
                veiculo.id
            ) {
                resetarFormularioVeiculo();
            }

            await onAtualizar();

        } catch (error) {

            tratarErro(
                error,
                'Não foi possível excluir o veículo.'
            );

        } finally {

            setSalvando(false);
        }
    };

    const cadastrarOuAtualizarMotorista =
        async (event) => {

            event.preventDefault();

            limparMensagens();

            if (!nome.trim()) {
                setErro(
                    'Informe o nome do motorista.'
                );
                return;
            }

            if (cpf.length !== 11) {
                setErro(
                    'Informe um CPF com 11 dígitos.'
                );
                return;
            }

            setSalvando(true);

            try {

                const dados = {
                    nome: nome.trim(),
                    cpf,
                    tipoCNH
                };

                if (motoristaEditando) {

                    await api.put(
                        `/motoristas/${motoristaEditando}`,
                        dados
                    );

                    setMensagem(
                        'Motorista atualizado com sucesso.'
                    );

                } else {

                    await api.post(
                        '/motoristas',
                        dados
                    );

                    setMensagem(
                        'Motorista cadastrado com sucesso.'
                    );
                }

                resetarFormularioMotorista();

                await onAtualizar();

            } catch (error) {

                tratarErro(
                    error,
                    'Não foi possível salvar o motorista.'
                );

            } finally {

                setSalvando(false);
            }
        };

    const cadastrarOuAtualizarVeiculo =
        async (event) => {

            event.preventDefault();

            limparMensagens();

            const capacidadeNumero =
                Number(capacidade);

            if (!placa.trim()) {
                setErro(
                    'Informe a placa do veículo.'
                );
                return;
            }

            if (!modelo.trim()) {
                setErro(
                    'Informe o modelo do veículo.'
                );
                return;
            }

            if (
                !Number.isFinite(
                    capacidadeNumero
                ) ||
                capacidadeNumero <= 0
            ) {
                setErro(
                    'Informe uma capacidade de carga válida.'
                );
                return;
            }

            let endpoint;

            let dados;

            if (
                tipoVeiculo ===
                'caminhao'
            ) {

                const eixos =
                    Number(
                        quantidadeEixos
                    );

                if (
                    !Number.isInteger(eixos) ||
                    eixos <= 0
                ) {
                    setErro(
                        'Informe uma quantidade de eixos válida.'
                    );
                    return;
                }

                endpoint =
                    veiculoEditando
                        ? `/veiculos/caminhao/${veiculoEditando}`
                        : '/veiculos/caminhao';

                dados = {
                    placa:
                        placa.trim(),
                    modelo:
                        modelo.trim(),
                    capacidadeCargaKg:
                    capacidadeNumero,
                    quantidadeEixos:
                    eixos
                };

            } else {

                const volume =
                    Number(volumeM3);

                if (
                    !Number.isFinite(volume) ||
                    volume <= 0
                ) {
                    setErro(
                        'Informe um volume válido.'
                    );
                    return;
                }

                endpoint =
                    veiculoEditando
                        ? `/veiculos/furgao/${veiculoEditando}`
                        : '/veiculos/furgao';

                dados = {
                    placa:
                        placa.trim(),
                    modelo:
                        modelo.trim(),
                    capacidadeCargaKg:
                    capacidadeNumero,
                    volumeM3:
                    volume
                };
            }

            setSalvando(true);

            try {

                if (veiculoEditando) {

                    await api.put(
                        endpoint,
                        dados
                    );

                    setMensagem(
                        'Veículo atualizado com sucesso.'
                    );

                } else {

                    await api.post(
                        endpoint,
                        dados
                    );

                    setMensagem(
                        'Veículo cadastrado com sucesso.'
                    );
                }

                resetarFormularioVeiculo();

                await onAtualizar();

            } catch (error) {

                tratarErro(
                    error,
                    'Não foi possível salvar o veículo.'
                );

            } finally {

                setSalvando(false);
            }
        };

    return (
        <div
            className={fechando ? 'sgfl-modal-transition-exit' : 'sgfl-modal-transition-enter'}
            style={{
                position: 'fixed',
                inset: 0,
                backgroundColor:
                    'rgba(0, 0, 0, 0.55)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '20px',
                zIndex: 2000
            }}
        >
            <div
                style={{
                    width: '100%',
                    maxWidth: '900px',
                    maxHeight: '90vh',
                    overflowY: 'auto',
                    backgroundColor:
                    theme.surface,
                    border:
                        `1px solid ${theme.border}`,
                    borderRadius: '14px',
                    padding: '24px'
                }}
            >

                <div
                    style={{
                        display: 'flex',
                        justifyContent:
                            'space-between',
                        alignItems:
                            'flex-start',
                        gap: '20px',
                        marginBottom:
                            '20px'
                    }}
                >

                    <div>
                        <h2
                            style={{
                                margin: 0,
                                fontFamily:
                                    "Georgia, 'Iowan Old Style', serif",
                                fontSize: '21px',
                                fontWeight: 400
                            }}
                        >
                            Gerenciar recursos
                        </h2>

                        <p
                            style={{
                                margin:
                                    '6px 0 0',
                                color:
                                theme.inkSoft,
                                fontSize:
                                    '13px'
                            }}
                        >
                            Cadastre, edite ou exclua
                            motoristas e veículos.
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={fechar}
                        disabled={salvando || fechando}
                        style={{
                            width: '34px',
                            height: '34px',
                            borderRadius: '8px',
                            border:
                                `1px solid ${theme.border}`,
                            backgroundColor:
                            theme.surfaceAlt,
                            color: theme.ink,
                            fontSize: '22px',
                            cursor: 'pointer'
                        }}
                    >
                        ×
                    </button>

                </div>

                <div
                    style={{
                        display: 'flex',
                        gap: '8px',
                        marginBottom: '20px'
                    }}
                >

                    <button
                        type="button"
                        disabled={transicionando}
                        onClick={() => {
                            setAba(
                                'motorista'
                            );
                            limparMensagens();
                        }}
                        style={{
                            padding:
                                '9px 14px',
                            borderRadius:
                                '8px',
                            border:
                                `1px solid ${theme.border}`,
                            backgroundColor:
                                aba ===
                                'motorista'
                                    ? theme.accent
                                    : theme.surfaceAlt,
                            color:
                                aba ===
                                'motorista'
                                    ? theme.accentInk
                                    : theme.ink,
                            cursor:
                                'pointer',
                            fontWeight:
                                600
                        }}
                    >
                        Motoristas
                    </button>

                    <button
                        type="button"
                        disabled={transicionando}
                        onClick={() => {
                            setAba(
                                'veiculo'
                            );
                            limparMensagens();
                        }}
                        style={{
                            padding:
                                '9px 14px',
                            borderRadius:
                                '8px',
                            border:
                                `1px solid ${theme.border}`,
                            backgroundColor:
                                aba ===
                                'veiculo'
                                    ? theme.accent
                                    : theme.surfaceAlt,
                            color:
                                aba ===
                                'veiculo'
                                    ? theme.accentInk
                                    : theme.ink,
                            cursor:
                                'pointer',
                            fontWeight:
                                600
                        }}
                    >
                        Veículos
                    </button>

                </div>

                {mensagem && (
                    <div
                        style={{
                            padding:
                                '11px 13px',
                            marginBottom:
                                '14px',
                            borderRadius:
                                '8px',
                            backgroundColor:
                            theme.statuses.ENTREGUE.bg,
                            color:
                            theme.statuses.ENTREGUE.ink,
                            border:
                                `1px solid ${theme.border}`
                        }}
                    >
                        {mensagem}
                    </div>
                )}

                {erro && (
                    <div
                        style={{
                            padding:
                                '11px 13px',
                            marginBottom:
                                '14px',
                            borderRadius:
                                '8px',
                            backgroundColor:
                            theme.statuses.PENDENTE.bg,
                            color:
                            theme.statuses.PENDENTE.ink,
                            border:
                                `1px solid ${theme.border}`
                        }}
                    >
                        {erro}
                    </div>
                )}

                <div key={aba} className={classePainelAba}>
                {aba === 'motorista' && (
                    <>
                        <form
                            onSubmit={
                                cadastrarOuAtualizarMotorista
                            }
                            style={{
                                display:
                                    'grid',
                                gridTemplateColumns:
                                    '1fr 1fr 160px auto',
                                gap:
                                    '12px',
                                alignItems:
                                    'end',
                                marginBottom:
                                    '22px'
                            }}
                        >

                            <label
                                style={
                                    labelStyle(theme)
                                }
                            >
                                Nome

                                <input
                                    value={
                                        nome
                                    }
                                    onChange={
                                        (e) =>
                                            setNome(
                                                e.target.value
                                            )
                                    }
                                    style={
                                        inputStyle(theme)
                                    }
                                    disabled={
                                        salvando
                                    }
                                    placeholder="Nome completo"
                                />
                            </label>

                            <label
                                style={
                                    labelStyle(theme)
                                }
                            >
                                CPF

                                <input
                                    value={
                                        cpf
                                    }
                                    onChange={
                                        handleCpfChange
                                    }
                                    style={
                                        inputStyle(theme)
                                    }
                                    disabled={
                                        salvando
                                    }
                                    placeholder="Somente números"
                                    inputMode="numeric"
                                    maxLength="11"
                                />
                            </label>

                            <label
                                style={
                                    labelStyle(theme)
                                }
                            >
                                CNH

                                <select
                                    value={
                                        tipoCNH
                                    }
                                    onChange={
                                        (e) =>
                                            setTipoCNH(
                                                e.target.value
                                            )
                                    }
                                    style={
                                        inputStyle(theme)
                                    }
                                    disabled={
                                        salvando
                                    }
                                >
                                    <option value="A">
                                        A
                                    </option>

                                    <option value="B">
                                        B
                                    </option>

                                    <option value="C">
                                        C
                                    </option>

                                    <option value="D">
                                        D
                                    </option>

                                    <option value="E">
                                        E
                                    </option>
                                </select>
                            </label>

                            <div
                                style={{
                                    display:
                                        'flex',
                                    gap: '6px'
                                }}
                            >
                                <button
                                    type="submit"
                                    disabled={
                                        salvando
                                    }
                                    style={
                                        primaryButton(theme)
                                    }
                                >
                                    {motoristaEditando
                                        ? 'Salvar'
                                        : 'Cadastrar'}
                                </button>

                                {motoristaEditando && (
                                    <button
                                        type="button"
                                        onClick={
                                            resetarFormularioMotorista
                                        }
                                        disabled={
                                            salvando
                                        }
                                        style={
                                            secondaryButton(theme)
                                        }
                                    >
                                        Cancelar
                                    </button>
                                )}
                            </div>

                        </form>

                        <ResourceList
                            items={
                                motoristas
                            }
                            type="motorista"
                            theme={
                                theme
                            }
                            onEdit={
                                iniciarEdicaoMotorista
                            }
                            onDelete={
                                excluirMotorista
                            }
                            disabled={
                                salvando
                            }
                        />
                    </>
                )}

                {aba === 'veiculo' && (
                    <>
                        <form
                            onSubmit={
                                cadastrarOuAtualizarVeiculo
                            }
                            style={{
                                display:
                                    'grid',
                                gridTemplateColumns:
                                    '150px 1fr 1fr',
                                gap:
                                    '12px',
                                alignItems:
                                    'end',
                                marginBottom:
                                    '22px'
                            }}
                        >

                            <label
                                style={
                                    labelStyle(theme)
                                }
                            >
                                Tipo

                                <select
                                    value={
                                        tipoVeiculo
                                    }
                                    onChange={
                                        (e) => {
                                            setTipoVeiculo(
                                                e.target.value
                                            );

                                            if (
                                                e.target.value ===
                                                'caminhao'
                                            ) {
                                                setVolumeM3(
                                                    ''
                                                );
                                            } else {
                                                setQuantidadeEixos(
                                                    ''
                                                );
                                            }

                                            limparMensagens();
                                        }
                                    }
                                    style={
                                        inputStyle(theme)
                                    }
                                    disabled={
                                        salvando ||
                                        veiculoEditando !==
                                        null
                                    }
                                >
                                    <option value="caminhao">
                                        Caminhão
                                    </option>

                                    <option value="furgao">
                                        Furgão
                                    </option>
                                </select>
                            </label>

                            <label
                                style={
                                    labelStyle(theme)
                                }
                            >
                                Placa

                                <input
                                    value={
                                        placa
                                    }
                                    onChange={
                                        handlePlacaChange
                                    }
                                    style={
                                        inputStyle(theme)
                                    }
                                    disabled={
                                        salvando
                                    }
                                    placeholder="ABC1D23"
                                    maxLength="7"
                                />
                            </label>

                            <label
                                style={
                                    labelStyle(theme)
                                }
                            >
                                Modelo

                                <input
                                    value={
                                        modelo
                                    }
                                    onChange={
                                        (e) =>
                                            setModelo(
                                                e.target.value
                                            )
                                    }
                                    style={
                                        inputStyle(theme)
                                    }
                                    disabled={
                                        salvando
                                    }
                                    placeholder="Volvo FH 540"
                                />
                            </label>

                            <label
                                style={
                                    labelStyle(theme)
                                }
                            >
                                Capacidade (kg)

                                <input
                                    type="number"
                                    min="0.01"
                                    step="0.01"
                                    value={
                                        capacidade
                                    }
                                    onChange={
                                        (e) =>
                                            setCapacidade(
                                                e.target.value
                                            )
                                    }
                                    style={
                                        inputStyle(theme)
                                    }
                                    disabled={
                                        salvando
                                    }
                                />
                            </label>

                            {tipoVeiculo ===
                            'caminhao' ? (
                                <label
                                    style={
                                        labelStyle(theme)
                                    }
                                >
                                    Eixos

                                    <input
                                        type="number"
                                        min="1"
                                        step="1"
                                        value={
                                            quantidadeEixos
                                        }
                                        onChange={
                                            (e) =>
                                                setQuantidadeEixos(
                                                    e.target.value
                                                )
                                        }
                                        style={
                                            inputStyle(theme)
                                        }
                                        disabled={
                                            salvando
                                        }
                                    />
                                </label>
                            ) : (
                                <label
                                    style={
                                        labelStyle(theme)
                                    }
                                >
                                    Volume (m³)

                                    <input
                                        type="number"
                                        min="0.01"
                                        step="0.01"
                                        value={
                                            volumeM3
                                        }
                                        onChange={
                                            (e) =>
                                                setVolumeM3(
                                                    e.target.value
                                                )
                                        }
                                        style={
                                            inputStyle(theme)
                                        }
                                        disabled={
                                            salvando
                                        }
                                    />
                                </label>
                            )}

                            <div
                                style={{
                                    display:
                                        'flex',
                                    gap:
                                        '6px'
                                }}
                            >
                                <button
                                    type="submit"
                                    disabled={
                                        salvando
                                    }
                                    style={
                                        primaryButton(theme)
                                    }
                                >
                                    {veiculoEditando
                                        ? 'Salvar'
                                        : 'Cadastrar'}
                                </button>

                                {veiculoEditando && (
                                    <button
                                        type="button"
                                        onClick={
                                            resetarFormularioVeiculo
                                        }
                                        disabled={
                                            salvando
                                        }
                                        style={
                                            secondaryButton(theme)
                                        }
                                    >
                                        Cancelar
                                    </button>
                                )}
                            </div>

                        </form>

                        <ResourceList
                            items={
                                veiculos
                            }
                            type="veiculo"
                            theme={
                                theme
                            }
                            onEdit={
                                iniciarEdicaoVeiculo
                            }
                            onDelete={
                                excluirVeiculo
                            }
                            disabled={
                                salvando
                            }
                        />
                    </>
                )}

                </div>

            </div>
        </div>
    );
}

function ResourceList({
                          items,
                          type,
                          theme,
                          onEdit,
                          onDelete,
                          disabled
                      }) {

    if (!items.length) {
        return (
            <div
                style={{
                    padding: '18px',
                    border:
                        `1px solid ${theme.border}`,
                    borderRadius: '8px',
                    color:
                    theme.inkSoft,
                    fontSize: '13px'
                }}
            >
                Nenhum cadastro encontrado.
            </div>
        );
    }

    return (
        <div
            style={{
                display:
                    'flex',
                flexDirection:
                    'column',
                gap:
                    '8px'
            }}
        >
            {items.map(
                (item) => {

                    if (
                        type ===
                        'motorista'
                    ) {
                        return (
                            <div
                                key={
                                    item.id
                                }
                                style={{
                                    display:
                                        'flex',
                                    justifyContent:
                                        'space-between',
                                    alignItems:
                                        'center',
                                    gap:
                                        '14px',
                                    padding:
                                        '12px',
                                    border:
                                        `1px solid ${theme.border}`,
                                    borderRadius:
                                        '8px',
                                    backgroundColor:
                                    theme.surfaceAlt
                                }}
                            >
                                <div>
                                    <strong>
                                        {
                                            item.nome
                                        }
                                    </strong>

                                    <div
                                        style={{
                                            marginTop:
                                                '5px',
                                            color:
                                            theme.inkSoft,
                                            fontSize:
                                                '12px'
                                        }}
                                    >
                                        CPF:{' '}
                                        {
                                            item.cpf
                                        }
                                        {' · '}
                                        CNH:{' '}
                                        {
                                            item.tipoCNH
                                        }
                                    </div>
                                </div>

                                <div
                                    style={{
                                        display:
                                            'flex',
                                        gap:
                                            '6px'
                                    }}
                                >
                                    <button
                                        type="button"
                                        disabled={
                                            disabled
                                        }
                                        onClick={() =>
                                            onEdit(
                                                item
                                            )
                                        }
                                        style={
                                            secondaryButton(theme)
                                        }
                                    >
                                        Editar
                                    </button>

                                    <button
                                        type="button"
                                        disabled={
                                            disabled
                                        }
                                        onClick={() =>
                                            onDelete(
                                                item
                                            )
                                        }
                                        style={
                                            dangerButton(theme)
                                        }
                                    >
                                        Excluir
                                    </button>
                                </div>
                            </div>
                        );
                    }

                    const ehCaminhao =
                        item.quantidadeEixos !==
                        undefined &&
                        item.quantidadeEixos !==
                        null;

                    return (
                        <div
                            key={
                                item.id
                            }
                            style={{
                                display:
                                    'flex',
                                justifyContent:
                                    'space-between',
                                alignItems:
                                    'center',
                                gap:
                                    '14px',
                                padding:
                                    '12px',
                                border:
                                    `1px solid ${theme.border}`,
                                borderRadius:
                                    '8px',
                                backgroundColor:
                                theme.surfaceAlt
                            }}
                        >
                            <div>
                                <strong>
                                    {
                                        item.modelo
                                    }
                                </strong>

                                <div
                                    style={{
                                        marginTop:
                                            '5px',
                                        color:
                                        theme.inkSoft,
                                        fontSize:
                                            '12px'
                                    }}
                                >
                                    {ehCaminhao
                                        ? 'Caminhão'
                                        : 'Furgão'}
                                    {' · '}
                                    {
                                        item.placa
                                    }
                                    {' · '}
                                    {
                                        Number(
                                            item.capacidadeCargaKg
                                        ).toLocaleString(
                                            'pt-BR'
                                        )
                                    }{' '}
                                    kg
                                </div>
                            </div>

                            <div
                                style={{
                                    display:
                                        'flex',
                                    gap:
                                        '6px'
                                }}
                            >
                                <button
                                    type="button"
                                    disabled={
                                        disabled
                                    }
                                    onClick={() =>
                                        onEdit(
                                            item
                                        )
                                    }
                                    style={
                                        secondaryButton(theme)
                                    }
                                >
                                    Editar
                                </button>

                                <button
                                    type="button"
                                    disabled={
                                        disabled
                                    }
                                    onClick={() =>
                                        onDelete(
                                            item
                                        )
                                    }
                                    style={
                                        dangerButton(theme)
                                    }
                                >
                                    Excluir
                                </button>
                            </div>
                        </div>
                    );
                }
            )}
        </div>
    );
}

function labelStyle(theme) {
    return {
        display:
            'flex',
        flexDirection:
            'column',
        gap:
            '6px',
        fontSize:
            '13px',
        color:
        theme.inkSoft,
        fontWeight:
            500
    };
}

function inputStyle(theme) {
    return {
        padding:
            '10px 12px',
        borderRadius:
            '8px',
        border:
            `1px solid ${theme.borderStrong}`,
        backgroundColor:
        theme.surfaceAlt,
        color:
        theme.ink,
        fontSize:
            '14px'
    };
}

function primaryButton(theme) {
    return {
        padding:
            '10px 14px',
        border:
            'none',
        borderRadius:
            '8px',
        backgroundColor:
        theme.accent,
        color:
        theme.accentInk,
        fontWeight:
            600,
        cursor:
            'pointer'
    };
}

function secondaryButton(theme) {
    return {
        padding:
            '8px 12px',
        borderRadius:
            '8px',
        border:
            `1px solid ${theme.border}`,
        backgroundColor:
        theme.surface,
        color:
        theme.ink,
        fontWeight:
            600,
        cursor:
            'pointer'
    };
}

function dangerButton(theme) {
    return {
        padding:
            '8px 12px',
        borderRadius:
            '8px',
        border:
            `1px solid ${theme.border}`,
        backgroundColor:
            'transparent',
        color:
        theme.danger,
        fontWeight:
            600,
        cursor:
            'pointer'
    };
}
