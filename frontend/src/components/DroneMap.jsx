import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const alertIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
  iconSize: [25, 41], iconAnchor: [12, 41]
});

export default function DroneMap({ aircraft, zones, alertIcaos }) {
  return (
    <MapContainer center={[40.4, -3.7]} zoom={6}
      style={{ height: '500px', width: '100%', borderRadius: '8px' }}>
      <TileLayer url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
        attribution='OpenStreetMap contributors' />
      {zones.map(z => (
        <Circle key={z.id} center={[z.latitude, z.longitude]}
          radius={z.radiusKm * 1000}
          pathOptions={{ color: 'red', fillColor: 'red', fillOpacity: 0.1 }}>
          <Popup>{z.name} — {z.type}</Popup>
        </Circle>
      ))}
      {aircraft.filter(a => a.latitude && a.longitude).map(a => (
        <Marker key={a.icao24} position={[a.latitude, a.longitude]}
          icon={alertIcaos.has(a.icao24) ? alertIcon : undefined}>
          <Popup>
            <strong>{a.callsign}</strong><br/>
            {alertIcaos.has(a.icao24) &&
              <span style={{color:'red',fontWeight:'bold'}}>ALERTA ACTIVA</span>}
            <br/>Alt: {a.altitude ? Math.round(a.altitude) + ' m' : 'N/A'}
            <br/>Vel: {a.velocity ? Math.round(a.velocity * 3.6) + ' km/h' : 'N/A'}
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
