import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

/**
 * Mapa da entrega com OpenStreetMap (sem chave de API).
 *
 * Renderiza os marcadores de origem/destino e a linha da rota quando a
 * entrega já tem coordenadas; caso contrário mostra instrução de
 * geocodificação. Quando o provedor devolve a geometria real
 * (polyline do OSRM ou pontos do TomTom), a linha segue as estradas;
 * sem geometria, desenhamos a reta estimada (tracejada).
 *
 * No tema escuro ({@code escuro}) os tiles trocam para o Esri Dark
 * Gray Canvas (Base + rotulos Reference), gratuito e sem chave, com
 * visual de editor preto/cinza; no claro, tiles OSM padrão. Marcadores
 * e rota sempre seguem as cores do tema atual.
 */
export default function MapaEntrega({ coordenadas, rota, tema, escuro = false }) {

    const containerRef = useRef(null);

    const corOrigem = tema?.accent || '#A54552';
    const corDestino = tema?.statuses?.ENTREGUE?.dot || '#2E7D32';

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

        const geometriaReal =
            rota?.geometria && rota.geometria.length >= 2
                ? rota.geometria.map(ponto => [ponto.latitude, ponto.longitude])
                : null;

        const linha = geometriaReal ?? [origem, destino];

        const mapa = L.map(containerRef.current, {
            scrollWheelZoom: false,
            attributionControl: true
        });

        if (escuro) {
            // Basemap escuro: Esri Dark Gray Canvas (sem chave) com tinta
            // azul marinho aplicada por CSS (.mapa-escura .leaflet-tile-pane)
            // — visual estilo Google Maps/CARTO dark, sem watermark. A
            // camada Reference compoe os rotulos por cima da Base.
            L.tileLayer(
                'https://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}',
                {
                    maxZoom: 16,
                    attribution:
                        '&copy; Esri, HERE, Garmin, FAO, NOAA, USGS, OpenStreetMap contributors, GIS User Community'
                }
            ).addTo(mapa);
            L.tileLayer(
                'https://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}',
                { maxZoom: 16 }
            ).addTo(mapa);
        } else {
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(mapa);
        }

        const marcador = (cor) =>
            L.divIcon({
                className: '',
                html:
                    `<span style="display:block;width:16px;height:16px;border-radius:50%;background:${cor};border:3px solid #fff;box-shadow:0 0 6px rgba(0,0,0,.45);"></span>`,
                iconSize: [16, 16],
                iconAnchor: [8, 8]
            });

        L.marker(origem, { icon: marcador(corOrigem) })
            .addTo(mapa)
            .bindPopup('<strong>Origem</strong>');

        L.marker(destino, { icon: marcador(corDestino) })
            .addTo(mapa)
            .bindPopup('<strong>Destino</strong>');

        // Rota real = linha contínua; estimativa em linha reta = tracejada.
        L.polyline(linha, {
            color: corOrigem,
            weight: 3,
            dashArray: geometriaReal ? null : '6 8'
        }).addTo(mapa);

        mapa.fitBounds(L.latLngBounds(linha).pad(0.35));

        return () => {
            mapa.remove();
        };
    }, [latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino,
        possuiCoordenadas, rota, escuro, corOrigem, corDestino]);

    const classeMapa = escuro ? 'mapa-escura' : '';

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
                className={classeMapa}
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
