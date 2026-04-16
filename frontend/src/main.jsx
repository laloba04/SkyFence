import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'

// Bloquear scroll global — solo scrollea el <main> del Layout
const globalStyle = document.createElement('style');
globalStyle.textContent = `
  html, body, #root {
    height: 100%;
    margin: 0;
    overflow: hidden;
  }
`;
document.head.appendChild(globalStyle);

// Solución robusta para Leaflet: Configurar antes de inicializar React
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Icono SVG en línea para pantallas Retina y evitar problemas de empaquetado
const droneSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="28" height="28" fill="#d32f2f">
  <path d="M21,16v-2l-8-5V3.5C13,2.67,12.33,2,11.5,2S10,2.67,10,3.5V9l-8,5v2l8-2.5V19l-2,1.5V22l3.5-1l3.5,1v-1.5L13,19v-5.5L21,16z"/>
</svg>`;

const svgIcon = L.divIcon({
    html: droneSvg,
    className: '', // Importante: evita el recuadro blanco por defecto de divIcon
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -14]
});
L.Marker.prototype.options.icon = svgIcon;

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
