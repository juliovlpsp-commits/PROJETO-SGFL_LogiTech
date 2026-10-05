import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

/**
 * Mapa da entrega com OpenStreetMap (sem chave de API).
 *
 * Renderiza os marcadores de origem/destino e a linha da rota quando a
 * entrega já tem coordenadas; caso contrário mostra instrução de
 * geocodificação. A rota exibida vem do backend (provedor OSRM ou
 * Haversine) — aqui só desenhamos os pontos.
 */
export default function MapaEntrega({ coordenadas, rota, tema }) {

    const containerRef = useRef(null);

    const {
        latitudeOrigem,
        longitudeOrigem,
        latitudeDestino,
        longitudeDestino
    } = coordenadas || {};

    const possuiCoordenadas =
        latitudeOrigem != null &&
        longitudeOrigem != null &&
        latitudeDestino != null &&
        longitudeDestino != null;

    useEffect(() => {
        if (!containerRef.current || !possuiCoordenadas) return undefined;

        const origem = [latitudeOrigem, longitudeOrigem];
        const destino = [latitudeDestino, longitudeDestino];

        const mapa = L.map(containerRef.current, {
            scrollWheelZoom: false,
            attributionControl: true
        });

        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(mapa);

        const marcador = (cor) =>
            L.divIcon({
                className: '',
                html:
                    `<span style="display:block;width:16px;height:16px;border-radius:50%;background:${cor};border:3px solid #fff;box-shadow:0 0 6px rgba(0,0,0,.45);"></span>`,
                iconSize: [16, 16],
                iconAnchor: [8, 8]
            });

        L.marker(origem, { icon: marcador('#A54552') })
            .addTo(mapa)
            .bindPopup('<strong>Origem</strong>');

        L.marker(destino, { icon: marcador('#2E7D32') })
            .addTo(mapa)
            .bindPopup('<strong>Destino</strong>');

        L.polyline([origem, destino], {
            color: '#A54552',
            weight: 3,
            dashArray: '6 8'
        }).addTo(mapa);

        mapa.fitBounds(L.latLngBounds([origem, destino]).pad(0.35));

        return () => {
            mapa.remove();
        };
    }, [latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino, possuiCoordenadas]);

    if (!possuiCoordenadas) {
        return (
            <div
                style={{
                    height: '220px',
                    display: 'grid',
                    placeItems: 'center',
                    borderRadius: '12px',
                    border: `1px dashed ${tema?.border || '#D9D2D6'}`,
                    background: tema?.surfaceAlt || '#FFFFFF',
                    color: tema?.inkSoft || '#6B6B72',
                    fontSize: '11px',
                    textAlign: 'center',
                    padding: '16px'
                }}
            >
                Sem coordenadas para exibir. Use
                &quot;Geocodificar endereços&quot; para preencher a rota.
            </div>
        );
    }

    return (
        <div>
            <div
                ref={containerRef}
                style={{
                    height: '320px',
                    width: '100%',
                    borderRadius: '12px',
                    overflow: 'hidden',
                    border: `1px solid ${tema?.border || '#D9D2D6'}`
                }}
            />

            {rota && rota.possuiCoordenadas && (
                <div
                    style={{
                        marginTop: '10px',
                        display: 'flex',
                        flexWrap: 'wrap',
                        gap: '8px 16px',
                        fontSize: '11px',
                        color: tema?.inkSoft || '#6B6B72'
                    }}
                >
                    <span>
                        <strong style={{ color: tema?.ink || '#1B1B1F' }}>
                            {rota.distanciaKm} km
                        </strong>
                    </span>
                    <span>
                        <strong style={{ color: tema?.ink || '#1B1B1F' }}>
                            {rota.duracaoMinutos} min
                        </strong>
                    </span>
                    <span>Fonte: {rota.fonte}</span>
                    {rota.previsaoChegada && (
                        <span>
                            Previsão: {new Date(rota.previsaoChegada).toLocaleString('pt-BR')}
                        </span>
                    )}
                </div>
            )}
        </div>
    );
}
