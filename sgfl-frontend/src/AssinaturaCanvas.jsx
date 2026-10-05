import { useEffect, useRef, useState } from 'react';

/**
 * Canvas de assinatura com dedo/mouse para o comprovante de entrega.
 *
 * Usa eventos de ponteiro (touch + mouse) e `touch-action: none` para
 * assinar sem rolar a página no celular. Devolve a assinatura como
 * dataURL (PNG) via `onChange`.
 */
export default function AssinaturaCanvas({ onChange, tema }) {

    const canvasRef = useRef(null);
    const desenhandoRef = useRef(false);
    const [vazio, setVazio] = useState(true);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return undefined;

        const contexto = canvas.getContext('2d');
        if (!contexto) return undefined;

        const razao = window.devicePixelRatio || 1;
        const largura = canvas.clientWidth;
        const altura = canvas.clientHeight;

        canvas.width = Math.round(largura * razao);
        canvas.height = Math.round(altura * razao);
        contexto.scale(razao, razao);

        contexto.lineWidth = 2.2;
        contexto.lineCap = 'round';
        contexto.lineJoin = 'round';
        contexto.strokeStyle = '#A54552';

        return undefined;
    }, []);

    const obterPonto = (event) => {
        const retangulo = canvasRef.current.getBoundingClientRect();
        return {
            x: event.clientX - retangulo.left,
            y: event.clientY - retangulo.top
        };
    };

    const iniciar = (event) => {
        event.preventDefault();
        const contexto = canvasRef.current.getContext('2d');
        const ponto = obterPonto(event);

        desenhandoRef.current = true;
        contexto.beginPath();
        contexto.moveTo(ponto.x, ponto.y);

        if (canvasRef.current.setPointerCapture) {
            canvasRef.current.setPointerCapture(event.pointerId);
        }
    };

    const desenhar = (event) => {
        if (!desenhandoRef.current) return;

        const contexto = canvasRef.current.getContext('2d');
        const ponto = obterPonto(event);

        contexto.lineTo(ponto.x, ponto.y);
        contexto.stroke();
    };

    const encerrar = () => {
        if (!desenhandoRef.current) return;

        desenhandoRef.current = false;
        setVazio(false);
        onChange?.(canvasRef.current.toDataURL('image/png'));
    };

    const limpar = () => {
        const canvas = canvasRef.current;
        const contexto = canvas.getContext('2d');

        contexto.clearRect(0, 0, canvas.width, canvas.height);
        setVazio(true);
        onChange?.('');
    };

    return (
        <div>
            <div style={{ position: 'relative' }}>
                <canvas
                    ref={canvasRef}
                    onPointerDown={iniciar}
                    onPointerMove={desenhar}
                    onPointerUp={encerrar}
                    onPointerLeave={encerrar}
                    style={{
                        width: '100%',
                        height: '140px',
                        display: 'block',
                        touchAction: 'none',
                        cursor: 'crosshair',
                        borderRadius: '10px',
                        border: `1px dashed ${tema?.border || '#D9D2D6'}`,
                        background: tema?.surfaceAlt || '#FFFFFF'
                    }}
                />
                {vazio && (
                    <div
                        style={{
                            position: 'absolute',
                            inset: 0,
                            display: 'grid',
                            placeItems: 'center',
                            pointerEvents: 'none',
                            fontSize: '11px',
                            color: tema?.inkSoft || '#6B6B72'
                        }}
                    >
                        Assine com o dedo ou o mouse
                    </div>
                )}
            </div>

            <button
                type="button"
                onClick={limpar}
                style={{
                    marginTop: '6px',
                    padding: '4px 10px',
                    fontSize: '10px',
                    borderRadius: '8px',
                    border: `1px solid ${tema?.border || '#D9D2D6'}`,
                    background: 'transparent',
                    color: tema?.inkSoft || '#6B6B72',
                    cursor: 'pointer'
                }}
            >
                Limpar assinatura
            </button>
        </div>
    );
}
