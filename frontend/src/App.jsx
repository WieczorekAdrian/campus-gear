import React from 'react';
import EquipmentList from './components/EquipmentList'; // Dodajemy import!

function App() {
  return (
      <div>
        <h1>Campus Gear - System Wypożyczalni</h1>
        <EquipmentList /> {/* Tutaj renderujemy naszą tabelkę */}
      </div>
  );
}

export default App;