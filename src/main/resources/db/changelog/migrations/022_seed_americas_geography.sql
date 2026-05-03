-- ============================================================
-- Seed: Americas geography (countries -> states -> cities)
-- ============================================================
-- Idempotent thanks to unique constraints:
--   countries.name UNIQUE
--   states (country_id, name) UNIQUE
--   cities (state_id, name) UNIQUE
-- All FKs resolved by name via subquery; no hardcoded IDs.
-- ============================================================

-- ============================================================
-- 1. Countries
-- ============================================================
INSERT IGNORE INTO countries (name) VALUES
('Antigua and Barbuda'),
('Argentina'),
('Bahamas'),
('Barbados'),
('Belize'),
('Bolivia'),
('Brazil'),
('Canada'),
('Chile'),
('Colombia'),
('Costa Rica'),
('Cuba'),
('Dominica'),
('Dominican Republic'),
('Ecuador'),
('El Salvador'),
('Grenada'),
('Guatemala'),
('Guyana'),
('Haiti'),
('Honduras'),
('Jamaica'),
('Mexico'),
('Nicaragua'),
('Panama'),
('Paraguay'),
('Peru'),
('Saint Kitts and Nevis'),
('Saint Lucia'),
('Saint Vincent and the Grenadines'),
('Suriname'),
('Trinidad and Tobago'),
('United States'),
('Uruguay'),
('Venezuela');

-- ============================================================
-- 2. States and cities, grouped by country
-- ============================================================

-- ------------------------------------------------------------
-- Antigua and Barbuda
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Saint George' AS name UNION ALL
    SELECT 'Saint John' UNION ALL
    SELECT 'Saint Mary' UNION ALL
    SELECT 'Saint Paul' UNION ALL
    SELECT 'Saint Peter' UNION ALL
    SELECT 'Saint Philip' UNION ALL
    SELECT 'Barbuda' UNION ALL
    SELECT 'Redonda'
) t WHERE c.name = 'Antigua and Barbuda';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Saint John' AS state_name, 'Saint John''s' AS name UNION ALL
    SELECT 'Saint George', 'Piggotts' UNION ALL
    SELECT 'Saint Mary', 'Bolans' UNION ALL
    SELECT 'Saint Paul', 'Falmouth' UNION ALL
    SELECT 'Saint Peter', 'Parham' UNION ALL
    SELECT 'Saint Philip', 'Willikies' UNION ALL
    SELECT 'Barbuda', 'Codrington'
) t ON t.state_name = s.name
WHERE c.name = 'Antigua and Barbuda';

-- ------------------------------------------------------------
-- Argentina
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Buenos Aires' AS name UNION ALL
    SELECT 'Ciudad Autónoma de Buenos Aires' UNION ALL
    SELECT 'Catamarca' UNION ALL
    SELECT 'Chaco' UNION ALL
    SELECT 'Chubut' UNION ALL
    SELECT 'Córdoba' UNION ALL
    SELECT 'Corrientes' UNION ALL
    SELECT 'Entre Ríos' UNION ALL
    SELECT 'Formosa' UNION ALL
    SELECT 'Jujuy' UNION ALL
    SELECT 'La Pampa' UNION ALL
    SELECT 'La Rioja' UNION ALL
    SELECT 'Mendoza' UNION ALL
    SELECT 'Misiones' UNION ALL
    SELECT 'Neuquén' UNION ALL
    SELECT 'Río Negro' UNION ALL
    SELECT 'Salta' UNION ALL
    SELECT 'San Juan' UNION ALL
    SELECT 'San Luis' UNION ALL
    SELECT 'Santa Cruz' UNION ALL
    SELECT 'Santa Fe' UNION ALL
    SELECT 'Santiago del Estero' UNION ALL
    SELECT 'Tierra del Fuego' UNION ALL
    SELECT 'Tucumán'
) t WHERE c.name = 'Argentina';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Buenos Aires' AS state_name, 'La Plata' AS name UNION ALL
    SELECT 'Buenos Aires', 'Mar del Plata' UNION ALL
    SELECT 'Buenos Aires', 'Bahía Blanca' UNION ALL
    SELECT 'Ciudad Autónoma de Buenos Aires', 'Buenos Aires' UNION ALL
    SELECT 'Catamarca', 'San Fernando del Valle de Catamarca' UNION ALL
    SELECT 'Chaco', 'Resistencia' UNION ALL
    SELECT 'Chubut', 'Rawson' UNION ALL
    SELECT 'Chubut', 'Comodoro Rivadavia' UNION ALL
    SELECT 'Chubut', 'Puerto Madryn' UNION ALL
    SELECT 'Córdoba', 'Córdoba' UNION ALL
    SELECT 'Córdoba', 'Villa Carlos Paz' UNION ALL
    SELECT 'Córdoba', 'Río Cuarto' UNION ALL
    SELECT 'Corrientes', 'Corrientes' UNION ALL
    SELECT 'Entre Ríos', 'Paraná' UNION ALL
    SELECT 'Entre Ríos', 'Concordia' UNION ALL
    SELECT 'Formosa', 'Formosa' UNION ALL
    SELECT 'Jujuy', 'San Salvador de Jujuy' UNION ALL
    SELECT 'La Pampa', 'Santa Rosa' UNION ALL
    SELECT 'La Rioja', 'La Rioja' UNION ALL
    SELECT 'Mendoza', 'Mendoza' UNION ALL
    SELECT 'Mendoza', 'San Rafael' UNION ALL
    SELECT 'Misiones', 'Posadas' UNION ALL
    SELECT 'Neuquén', 'Neuquén' UNION ALL
    SELECT 'Río Negro', 'Viedma' UNION ALL
    SELECT 'Río Negro', 'San Carlos de Bariloche' UNION ALL
    SELECT 'Salta', 'Salta' UNION ALL
    SELECT 'San Juan', 'San Juan' UNION ALL
    SELECT 'San Luis', 'San Luis' UNION ALL
    SELECT 'Santa Cruz', 'Río Gallegos' UNION ALL
    SELECT 'Santa Fe', 'Santa Fe' UNION ALL
    SELECT 'Santa Fe', 'Rosario' UNION ALL
    SELECT 'Santiago del Estero', 'Santiago del Estero' UNION ALL
    SELECT 'Tierra del Fuego', 'Ushuaia' UNION ALL
    SELECT 'Tierra del Fuego', 'Río Grande' UNION ALL
    SELECT 'Tucumán', 'San Miguel de Tucumán'
) t ON t.state_name = s.name
WHERE c.name = 'Argentina';

-- ------------------------------------------------------------
-- Bahamas
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'New Providence' AS name UNION ALL
    SELECT 'Grand Bahama' UNION ALL
    SELECT 'Abaco' UNION ALL
    SELECT 'Eleuthera' UNION ALL
    SELECT 'Andros' UNION ALL
    SELECT 'Bimini' UNION ALL
    SELECT 'Cat Island' UNION ALL
    SELECT 'Exuma' UNION ALL
    SELECT 'Inagua' UNION ALL
    SELECT 'Long Island' UNION ALL
    SELECT 'Mayaguana' UNION ALL
    SELECT 'Acklins' UNION ALL
    SELECT 'Crooked Island' UNION ALL
    SELECT 'Berry Islands' UNION ALL
    SELECT 'San Salvador' UNION ALL
    SELECT 'Ragged Island' UNION ALL
    SELECT 'Rum Cay'
) t WHERE c.name = 'Bahamas';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'New Providence' AS state_name, 'Nassau' AS name UNION ALL
    SELECT 'Grand Bahama', 'Freeport' UNION ALL
    SELECT 'Abaco', 'Marsh Harbour' UNION ALL
    SELECT 'Eleuthera', 'Governor''s Harbour' UNION ALL
    SELECT 'Andros', 'Andros Town' UNION ALL
    SELECT 'Bimini', 'Alice Town' UNION ALL
    SELECT 'Cat Island', 'Arthur''s Town' UNION ALL
    SELECT 'Exuma', 'George Town' UNION ALL
    SELECT 'Inagua', 'Matthew Town' UNION ALL
    SELECT 'Long Island', 'Clarence Town' UNION ALL
    SELECT 'San Salvador', 'Cockburn Town'
) t ON t.state_name = s.name
WHERE c.name = 'Bahamas';

-- ------------------------------------------------------------
-- Barbados
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Christ Church' AS name UNION ALL
    SELECT 'Saint Andrew' UNION ALL
    SELECT 'Saint George' UNION ALL
    SELECT 'Saint James' UNION ALL
    SELECT 'Saint John' UNION ALL
    SELECT 'Saint Joseph' UNION ALL
    SELECT 'Saint Lucy' UNION ALL
    SELECT 'Saint Michael' UNION ALL
    SELECT 'Saint Peter' UNION ALL
    SELECT 'Saint Philip' UNION ALL
    SELECT 'Saint Thomas'
) t WHERE c.name = 'Barbados';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Saint Michael' AS state_name, 'Bridgetown' AS name UNION ALL
    SELECT 'Christ Church', 'Oistins' UNION ALL
    SELECT 'Saint James', 'Holetown' UNION ALL
    SELECT 'Saint Peter', 'Speightstown' UNION ALL
    SELECT 'Saint John', 'Four Roads' UNION ALL
    SELECT 'Saint Philip', 'Six Cross Roads' UNION ALL
    SELECT 'Saint Lucy', 'Crab Hill' UNION ALL
    SELECT 'Saint Andrew', 'Greenland' UNION ALL
    SELECT 'Saint George', 'Bulkeley' UNION ALL
    SELECT 'Saint Joseph', 'Bathsheba' UNION ALL
    SELECT 'Saint Thomas', 'Welchman Hall'
) t ON t.state_name = s.name
WHERE c.name = 'Barbados';

-- ------------------------------------------------------------
-- Belize
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Belize' AS name UNION ALL
    SELECT 'Cayo' UNION ALL
    SELECT 'Corozal' UNION ALL
    SELECT 'Orange Walk' UNION ALL
    SELECT 'Stann Creek' UNION ALL
    SELECT 'Toledo'
) t WHERE c.name = 'Belize';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Belize' AS state_name, 'Belize City' AS name UNION ALL
    SELECT 'Belize', 'San Pedro' UNION ALL
    SELECT 'Cayo', 'Belmopan' UNION ALL
    SELECT 'Cayo', 'San Ignacio' UNION ALL
    SELECT 'Corozal', 'Corozal Town' UNION ALL
    SELECT 'Orange Walk', 'Orange Walk Town' UNION ALL
    SELECT 'Stann Creek', 'Dangriga' UNION ALL
    SELECT 'Toledo', 'Punta Gorda'
) t ON t.state_name = s.name
WHERE c.name = 'Belize';

-- ------------------------------------------------------------
-- Bolivia
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Beni' AS name UNION ALL
    SELECT 'Chuquisaca' UNION ALL
    SELECT 'Cochabamba' UNION ALL
    SELECT 'La Paz' UNION ALL
    SELECT 'Oruro' UNION ALL
    SELECT 'Pando' UNION ALL
    SELECT 'Potosí' UNION ALL
    SELECT 'Santa Cruz' UNION ALL
    SELECT 'Tarija'
) t WHERE c.name = 'Bolivia';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Beni' AS state_name, 'Trinidad' AS name UNION ALL
    SELECT 'Chuquisaca', 'Sucre' UNION ALL
    SELECT 'Cochabamba', 'Cochabamba' UNION ALL
    SELECT 'Cochabamba', 'Quillacollo' UNION ALL
    SELECT 'La Paz', 'La Paz' UNION ALL
    SELECT 'La Paz', 'El Alto' UNION ALL
    SELECT 'Oruro', 'Oruro' UNION ALL
    SELECT 'Pando', 'Cobija' UNION ALL
    SELECT 'Potosí', 'Potosí' UNION ALL
    SELECT 'Santa Cruz', 'Santa Cruz de la Sierra' UNION ALL
    SELECT 'Santa Cruz', 'Montero' UNION ALL
    SELECT 'Tarija', 'Tarija'
) t ON t.state_name = s.name
WHERE c.name = 'Bolivia';

-- ------------------------------------------------------------
-- Brazil
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Acre' AS name UNION ALL
    SELECT 'Alagoas' UNION ALL
    SELECT 'Amapá' UNION ALL
    SELECT 'Amazonas' UNION ALL
    SELECT 'Bahia' UNION ALL
    SELECT 'Ceará' UNION ALL
    SELECT 'Distrito Federal' UNION ALL
    SELECT 'Espírito Santo' UNION ALL
    SELECT 'Goiás' UNION ALL
    SELECT 'Maranhão' UNION ALL
    SELECT 'Mato Grosso' UNION ALL
    SELECT 'Mato Grosso do Sul' UNION ALL
    SELECT 'Minas Gerais' UNION ALL
    SELECT 'Pará' UNION ALL
    SELECT 'Paraíba' UNION ALL
    SELECT 'Paraná' UNION ALL
    SELECT 'Pernambuco' UNION ALL
    SELECT 'Piauí' UNION ALL
    SELECT 'Rio de Janeiro' UNION ALL
    SELECT 'Rio Grande do Norte' UNION ALL
    SELECT 'Rio Grande do Sul' UNION ALL
    SELECT 'Rondônia' UNION ALL
    SELECT 'Roraima' UNION ALL
    SELECT 'Santa Catarina' UNION ALL
    SELECT 'São Paulo' UNION ALL
    SELECT 'Sergipe' UNION ALL
    SELECT 'Tocantins'
) t WHERE c.name = 'Brazil';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Acre' AS state_name, 'Rio Branco' AS name UNION ALL
    SELECT 'Alagoas', 'Maceió' UNION ALL
    SELECT 'Amapá', 'Macapá' UNION ALL
    SELECT 'Amazonas', 'Manaus' UNION ALL
    SELECT 'Bahia', 'Salvador' UNION ALL
    SELECT 'Bahia', 'Feira de Santana' UNION ALL
    SELECT 'Ceará', 'Fortaleza' UNION ALL
    SELECT 'Distrito Federal', 'Brasília' UNION ALL
    SELECT 'Espírito Santo', 'Vitória' UNION ALL
    SELECT 'Espírito Santo', 'Vila Velha' UNION ALL
    SELECT 'Goiás', 'Goiânia' UNION ALL
    SELECT 'Maranhão', 'São Luís' UNION ALL
    SELECT 'Mato Grosso', 'Cuiabá' UNION ALL
    SELECT 'Mato Grosso do Sul', 'Campo Grande' UNION ALL
    SELECT 'Minas Gerais', 'Belo Horizonte' UNION ALL
    SELECT 'Minas Gerais', 'Uberlândia' UNION ALL
    SELECT 'Minas Gerais', 'Contagem' UNION ALL
    SELECT 'Pará', 'Belém' UNION ALL
    SELECT 'Paraíba', 'João Pessoa' UNION ALL
    SELECT 'Paraná', 'Curitiba' UNION ALL
    SELECT 'Paraná', 'Londrina' UNION ALL
    SELECT 'Paraná', 'Maringá' UNION ALL
    SELECT 'Pernambuco', 'Recife' UNION ALL
    SELECT 'Piauí', 'Teresina' UNION ALL
    SELECT 'Rio de Janeiro', 'Rio de Janeiro' UNION ALL
    SELECT 'Rio de Janeiro', 'Niterói' UNION ALL
    SELECT 'Rio de Janeiro', 'Nova Iguaçu' UNION ALL
    SELECT 'Rio Grande do Norte', 'Natal' UNION ALL
    SELECT 'Rio Grande do Sul', 'Porto Alegre' UNION ALL
    SELECT 'Rio Grande do Sul', 'Caxias do Sul' UNION ALL
    SELECT 'Rondônia', 'Porto Velho' UNION ALL
    SELECT 'Roraima', 'Boa Vista' UNION ALL
    SELECT 'Santa Catarina', 'Florianópolis' UNION ALL
    SELECT 'Santa Catarina', 'Joinville' UNION ALL
    SELECT 'São Paulo', 'São Paulo' UNION ALL
    SELECT 'São Paulo', 'Campinas' UNION ALL
    SELECT 'São Paulo', 'Guarulhos' UNION ALL
    SELECT 'São Paulo', 'Santos' UNION ALL
    SELECT 'Sergipe', 'Aracaju' UNION ALL
    SELECT 'Tocantins', 'Palmas'
) t ON t.state_name = s.name
WHERE c.name = 'Brazil';

-- ------------------------------------------------------------
-- Canada
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Alberta' AS name UNION ALL
    SELECT 'British Columbia' UNION ALL
    SELECT 'Manitoba' UNION ALL
    SELECT 'New Brunswick' UNION ALL
    SELECT 'Newfoundland and Labrador' UNION ALL
    SELECT 'Nova Scotia' UNION ALL
    SELECT 'Ontario' UNION ALL
    SELECT 'Prince Edward Island' UNION ALL
    SELECT 'Quebec' UNION ALL
    SELECT 'Saskatchewan' UNION ALL
    SELECT 'Northwest Territories' UNION ALL
    SELECT 'Nunavut' UNION ALL
    SELECT 'Yukon'
) t WHERE c.name = 'Canada';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Alberta' AS state_name, 'Edmonton' AS name UNION ALL
    SELECT 'Alberta', 'Calgary' UNION ALL
    SELECT 'British Columbia', 'Victoria' UNION ALL
    SELECT 'British Columbia', 'Vancouver' UNION ALL
    SELECT 'Manitoba', 'Winnipeg' UNION ALL
    SELECT 'New Brunswick', 'Fredericton' UNION ALL
    SELECT 'New Brunswick', 'Moncton' UNION ALL
    SELECT 'Newfoundland and Labrador', 'St. John''s' UNION ALL
    SELECT 'Nova Scotia', 'Halifax' UNION ALL
    SELECT 'Ontario', 'Toronto' UNION ALL
    SELECT 'Ontario', 'Ottawa' UNION ALL
    SELECT 'Ontario', 'Mississauga' UNION ALL
    SELECT 'Prince Edward Island', 'Charlottetown' UNION ALL
    SELECT 'Quebec', 'Quebec City' UNION ALL
    SELECT 'Quebec', 'Montreal' UNION ALL
    SELECT 'Quebec', 'Laval' UNION ALL
    SELECT 'Saskatchewan', 'Regina' UNION ALL
    SELECT 'Saskatchewan', 'Saskatoon' UNION ALL
    SELECT 'Northwest Territories', 'Yellowknife' UNION ALL
    SELECT 'Nunavut', 'Iqaluit' UNION ALL
    SELECT 'Yukon', 'Whitehorse'
) t ON t.state_name = s.name
WHERE c.name = 'Canada';

-- ------------------------------------------------------------
-- Chile
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Arica y Parinacota' AS name UNION ALL
    SELECT 'Tarapacá' UNION ALL
    SELECT 'Antofagasta' UNION ALL
    SELECT 'Atacama' UNION ALL
    SELECT 'Coquimbo' UNION ALL
    SELECT 'Valparaíso' UNION ALL
    SELECT 'Metropolitana de Santiago' UNION ALL
    SELECT 'O''Higgins' UNION ALL
    SELECT 'Maule' UNION ALL
    SELECT 'Ñuble' UNION ALL
    SELECT 'Biobío' UNION ALL
    SELECT 'La Araucanía' UNION ALL
    SELECT 'Los Ríos' UNION ALL
    SELECT 'Los Lagos' UNION ALL
    SELECT 'Aysén' UNION ALL
    SELECT 'Magallanes y Antártica Chilena'
) t WHERE c.name = 'Chile';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Arica y Parinacota' AS state_name, 'Arica' AS name UNION ALL
    SELECT 'Tarapacá', 'Iquique' UNION ALL
    SELECT 'Antofagasta', 'Antofagasta' UNION ALL
    SELECT 'Antofagasta', 'Calama' UNION ALL
    SELECT 'Atacama', 'Copiapó' UNION ALL
    SELECT 'Coquimbo', 'La Serena' UNION ALL
    SELECT 'Coquimbo', 'Coquimbo' UNION ALL
    SELECT 'Valparaíso', 'Valparaíso' UNION ALL
    SELECT 'Valparaíso', 'Viña del Mar' UNION ALL
    SELECT 'Metropolitana de Santiago', 'Santiago' UNION ALL
    SELECT 'Metropolitana de Santiago', 'Puente Alto' UNION ALL
    SELECT 'Metropolitana de Santiago', 'Maipú' UNION ALL
    SELECT 'O''Higgins', 'Rancagua' UNION ALL
    SELECT 'Maule', 'Talca' UNION ALL
    SELECT 'Ñuble', 'Chillán' UNION ALL
    SELECT 'Biobío', 'Concepción' UNION ALL
    SELECT 'Biobío', 'Los Ángeles' UNION ALL
    SELECT 'La Araucanía', 'Temuco' UNION ALL
    SELECT 'Los Ríos', 'Valdivia' UNION ALL
    SELECT 'Los Lagos', 'Puerto Montt' UNION ALL
    SELECT 'Los Lagos', 'Osorno' UNION ALL
    SELECT 'Aysén', 'Coyhaique' UNION ALL
    SELECT 'Magallanes y Antártica Chilena', 'Punta Arenas'
) t ON t.state_name = s.name
WHERE c.name = 'Chile';

-- ------------------------------------------------------------
-- Colombia
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Amazonas' AS name UNION ALL
    SELECT 'Antioquia' UNION ALL
    SELECT 'Arauca' UNION ALL
    SELECT 'Atlántico' UNION ALL
    SELECT 'Bogotá D.C.' UNION ALL
    SELECT 'Bolívar' UNION ALL
    SELECT 'Boyacá' UNION ALL
    SELECT 'Caldas' UNION ALL
    SELECT 'Caquetá' UNION ALL
    SELECT 'Casanare' UNION ALL
    SELECT 'Cauca' UNION ALL
    SELECT 'Cesar' UNION ALL
    SELECT 'Chocó' UNION ALL
    SELECT 'Córdoba' UNION ALL
    SELECT 'Cundinamarca' UNION ALL
    SELECT 'Guainía' UNION ALL
    SELECT 'Guaviare' UNION ALL
    SELECT 'Huila' UNION ALL
    SELECT 'La Guajira' UNION ALL
    SELECT 'Magdalena' UNION ALL
    SELECT 'Meta' UNION ALL
    SELECT 'Nariño' UNION ALL
    SELECT 'Norte de Santander' UNION ALL
    SELECT 'Putumayo' UNION ALL
    SELECT 'Quindío' UNION ALL
    SELECT 'Risaralda' UNION ALL
    SELECT 'San Andrés y Providencia' UNION ALL
    SELECT 'Santander' UNION ALL
    SELECT 'Sucre' UNION ALL
    SELECT 'Tolima' UNION ALL
    SELECT 'Valle del Cauca' UNION ALL
    SELECT 'Vaupés' UNION ALL
    SELECT 'Vichada'
) t WHERE c.name = 'Colombia';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Amazonas' AS state_name, 'Leticia' AS name UNION ALL
    SELECT 'Antioquia', 'Medellín' UNION ALL
    SELECT 'Antioquia', 'Bello' UNION ALL
    SELECT 'Antioquia', 'Itagüí' UNION ALL
    SELECT 'Antioquia', 'Envigado' UNION ALL
    SELECT 'Arauca', 'Arauca' UNION ALL
    SELECT 'Atlántico', 'Barranquilla' UNION ALL
    SELECT 'Atlántico', 'Soledad' UNION ALL
    SELECT 'Bogotá D.C.', 'Bogotá' UNION ALL
    SELECT 'Bolívar', 'Cartagena' UNION ALL
    SELECT 'Bolívar', 'Magangué' UNION ALL
    SELECT 'Boyacá', 'Tunja' UNION ALL
    SELECT 'Boyacá', 'Duitama' UNION ALL
    SELECT 'Caldas', 'Manizales' UNION ALL
    SELECT 'Caquetá', 'Florencia' UNION ALL
    SELECT 'Casanare', 'Yopal' UNION ALL
    SELECT 'Cauca', 'Popayán' UNION ALL
    SELECT 'Cesar', 'Valledupar' UNION ALL
    SELECT 'Chocó', 'Quibdó' UNION ALL
    SELECT 'Córdoba', 'Montería' UNION ALL
    SELECT 'Cundinamarca', 'Soacha' UNION ALL
    SELECT 'Cundinamarca', 'Zipaquirá' UNION ALL
    SELECT 'Cundinamarca', 'Facatativá' UNION ALL
    SELECT 'Guainía', 'Inírida' UNION ALL
    SELECT 'Guaviare', 'San José del Guaviare' UNION ALL
    SELECT 'Huila', 'Neiva' UNION ALL
    SELECT 'La Guajira', 'Riohacha' UNION ALL
    SELECT 'La Guajira', 'Maicao' UNION ALL
    SELECT 'Magdalena', 'Santa Marta' UNION ALL
    SELECT 'Meta', 'Villavicencio' UNION ALL
    SELECT 'Nariño', 'Pasto' UNION ALL
    SELECT 'Nariño', 'Tumaco' UNION ALL
    SELECT 'Norte de Santander', 'Cúcuta' UNION ALL
    SELECT 'Putumayo', 'Mocoa' UNION ALL
    SELECT 'Quindío', 'Armenia' UNION ALL
    SELECT 'Risaralda', 'Pereira' UNION ALL
    SELECT 'San Andrés y Providencia', 'San Andrés' UNION ALL
    SELECT 'Santander', 'Bucaramanga' UNION ALL
    SELECT 'Santander', 'Floridablanca' UNION ALL
    SELECT 'Santander', 'Barrancabermeja' UNION ALL
    SELECT 'Sucre', 'Sincelejo' UNION ALL
    SELECT 'Tolima', 'Ibagué' UNION ALL
    SELECT 'Valle del Cauca', 'Cali' UNION ALL
    SELECT 'Valle del Cauca', 'Palmira' UNION ALL
    SELECT 'Valle del Cauca', 'Buenaventura' UNION ALL
    SELECT 'Vaupés', 'Mitú' UNION ALL
    SELECT 'Vichada', 'Puerto Carreño'
) t ON t.state_name = s.name
WHERE c.name = 'Colombia';

-- ------------------------------------------------------------
-- Costa Rica
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Alajuela' AS name UNION ALL
    SELECT 'Cartago' UNION ALL
    SELECT 'Guanacaste' UNION ALL
    SELECT 'Heredia' UNION ALL
    SELECT 'Limón' UNION ALL
    SELECT 'Puntarenas' UNION ALL
    SELECT 'San José'
) t WHERE c.name = 'Costa Rica';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'San José' AS state_name, 'San José' AS name UNION ALL
    SELECT 'Alajuela', 'Alajuela' UNION ALL
    SELECT 'Cartago', 'Cartago' UNION ALL
    SELECT 'Heredia', 'Heredia' UNION ALL
    SELECT 'Guanacaste', 'Liberia' UNION ALL
    SELECT 'Puntarenas', 'Puntarenas' UNION ALL
    SELECT 'Limón', 'Limón'
) t ON t.state_name = s.name
WHERE c.name = 'Costa Rica';

-- ------------------------------------------------------------
-- Cuba
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Pinar del Río' AS name UNION ALL
    SELECT 'Artemisa' UNION ALL
    SELECT 'La Habana' UNION ALL
    SELECT 'Mayabeque' UNION ALL
    SELECT 'Matanzas' UNION ALL
    SELECT 'Cienfuegos' UNION ALL
    SELECT 'Villa Clara' UNION ALL
    SELECT 'Sancti Spíritus' UNION ALL
    SELECT 'Ciego de Ávila' UNION ALL
    SELECT 'Camagüey' UNION ALL
    SELECT 'Las Tunas' UNION ALL
    SELECT 'Holguín' UNION ALL
    SELECT 'Granma' UNION ALL
    SELECT 'Santiago de Cuba' UNION ALL
    SELECT 'Guantánamo' UNION ALL
    SELECT 'Isla de la Juventud'
) t WHERE c.name = 'Cuba';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'La Habana' AS state_name, 'La Habana' AS name UNION ALL
    SELECT 'Pinar del Río', 'Pinar del Río' UNION ALL
    SELECT 'Artemisa', 'Artemisa' UNION ALL
    SELECT 'Mayabeque', 'San José de las Lajas' UNION ALL
    SELECT 'Matanzas', 'Matanzas' UNION ALL
    SELECT 'Matanzas', 'Varadero' UNION ALL
    SELECT 'Cienfuegos', 'Cienfuegos' UNION ALL
    SELECT 'Villa Clara', 'Santa Clara' UNION ALL
    SELECT 'Sancti Spíritus', 'Sancti Spíritus' UNION ALL
    SELECT 'Ciego de Ávila', 'Ciego de Ávila' UNION ALL
    SELECT 'Camagüey', 'Camagüey' UNION ALL
    SELECT 'Las Tunas', 'Las Tunas' UNION ALL
    SELECT 'Holguín', 'Holguín' UNION ALL
    SELECT 'Granma', 'Bayamo' UNION ALL
    SELECT 'Santiago de Cuba', 'Santiago de Cuba' UNION ALL
    SELECT 'Guantánamo', 'Guantánamo' UNION ALL
    SELECT 'Isla de la Juventud', 'Nueva Gerona'
) t ON t.state_name = s.name
WHERE c.name = 'Cuba';

-- ------------------------------------------------------------
-- Dominica
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Saint Andrew' AS name UNION ALL
    SELECT 'Saint David' UNION ALL
    SELECT 'Saint George' UNION ALL
    SELECT 'Saint John' UNION ALL
    SELECT 'Saint Joseph' UNION ALL
    SELECT 'Saint Luke' UNION ALL
    SELECT 'Saint Mark' UNION ALL
    SELECT 'Saint Patrick' UNION ALL
    SELECT 'Saint Paul' UNION ALL
    SELECT 'Saint Peter'
) t WHERE c.name = 'Dominica';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Saint George' AS state_name, 'Roseau' AS name UNION ALL
    SELECT 'Saint Andrew', 'Wesley' UNION ALL
    SELECT 'Saint David', 'Castle Bruce' UNION ALL
    SELECT 'Saint John', 'Portsmouth' UNION ALL
    SELECT 'Saint Joseph', 'Saint Joseph' UNION ALL
    SELECT 'Saint Luke', 'Pointe Michel' UNION ALL
    SELECT 'Saint Mark', 'Soufrière' UNION ALL
    SELECT 'Saint Patrick', 'Berekua' UNION ALL
    SELECT 'Saint Paul', 'Mahaut' UNION ALL
    SELECT 'Saint Peter', 'Colihaut'
) t ON t.state_name = s.name
WHERE c.name = 'Dominica';

-- ------------------------------------------------------------
-- Dominican Republic
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Distrito Nacional' AS name UNION ALL
    SELECT 'Azua' UNION ALL
    SELECT 'Baoruco' UNION ALL
    SELECT 'Barahona' UNION ALL
    SELECT 'Dajabón' UNION ALL
    SELECT 'Duarte' UNION ALL
    SELECT 'El Seibo' UNION ALL
    SELECT 'Elías Piña' UNION ALL
    SELECT 'Espaillat' UNION ALL
    SELECT 'Hato Mayor' UNION ALL
    SELECT 'Hermanas Mirabal' UNION ALL
    SELECT 'Independencia' UNION ALL
    SELECT 'La Altagracia' UNION ALL
    SELECT 'La Romana' UNION ALL
    SELECT 'La Vega' UNION ALL
    SELECT 'María Trinidad Sánchez' UNION ALL
    SELECT 'Monseñor Nouel' UNION ALL
    SELECT 'Monte Cristi' UNION ALL
    SELECT 'Monte Plata' UNION ALL
    SELECT 'Pedernales' UNION ALL
    SELECT 'Peravia' UNION ALL
    SELECT 'Puerto Plata' UNION ALL
    SELECT 'Samaná' UNION ALL
    SELECT 'San Cristóbal' UNION ALL
    SELECT 'San José de Ocoa' UNION ALL
    SELECT 'San Juan' UNION ALL
    SELECT 'San Pedro de Macorís' UNION ALL
    SELECT 'Sánchez Ramírez' UNION ALL
    SELECT 'Santiago' UNION ALL
    SELECT 'Santiago Rodríguez' UNION ALL
    SELECT 'Santo Domingo' UNION ALL
    SELECT 'Valverde'
) t WHERE c.name = 'Dominican Republic';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Distrito Nacional' AS state_name, 'Santo Domingo' AS name UNION ALL
    SELECT 'Santo Domingo', 'Santo Domingo Este' UNION ALL
    SELECT 'Santo Domingo', 'Santo Domingo Norte' UNION ALL
    SELECT 'Santo Domingo', 'Los Alcarrizos' UNION ALL
    SELECT 'Santiago', 'Santiago de los Caballeros' UNION ALL
    SELECT 'La Vega', 'La Vega' UNION ALL
    SELECT 'San Cristóbal', 'San Cristóbal' UNION ALL
    SELECT 'Puerto Plata', 'Puerto Plata' UNION ALL
    SELECT 'San Pedro de Macorís', 'San Pedro de Macorís' UNION ALL
    SELECT 'La Romana', 'La Romana' UNION ALL
    SELECT 'Duarte', 'San Francisco de Macorís' UNION ALL
    SELECT 'Azua', 'Azua' UNION ALL
    SELECT 'Barahona', 'Barahona' UNION ALL
    SELECT 'Espaillat', 'Moca' UNION ALL
    SELECT 'La Altagracia', 'Higüey' UNION ALL
    SELECT 'Monseñor Nouel', 'Bonao' UNION ALL
    SELECT 'Peravia', 'Baní' UNION ALL
    SELECT 'San Juan', 'San Juan de la Maguana' UNION ALL
    SELECT 'Valverde', 'Mao' UNION ALL
    SELECT 'Samaná', 'Samaná' UNION ALL
    SELECT 'Hato Mayor', 'Hato Mayor del Rey' UNION ALL
    SELECT 'Monte Plata', 'Monte Plata' UNION ALL
    SELECT 'El Seibo', 'El Seibo' UNION ALL
    SELECT 'Sánchez Ramírez', 'Cotuí' UNION ALL
    SELECT 'Hermanas Mirabal', 'Salcedo' UNION ALL
    SELECT 'María Trinidad Sánchez', 'Nagua' UNION ALL
    SELECT 'Baoruco', 'Neiba' UNION ALL
    SELECT 'Independencia', 'Jimaní' UNION ALL
    SELECT 'Pedernales', 'Pedernales' UNION ALL
    SELECT 'Elías Piña', 'Comendador' UNION ALL
    SELECT 'San José de Ocoa', 'San José de Ocoa' UNION ALL
    SELECT 'Dajabón', 'Dajabón' UNION ALL
    SELECT 'Monte Cristi', 'Monte Cristi' UNION ALL
    SELECT 'Santiago Rodríguez', 'Sabaneta'
) t ON t.state_name = s.name
WHERE c.name = 'Dominican Republic';

-- ------------------------------------------------------------
-- Ecuador
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Azuay' AS name UNION ALL
    SELECT 'Bolívar' UNION ALL
    SELECT 'Cañar' UNION ALL
    SELECT 'Carchi' UNION ALL
    SELECT 'Chimborazo' UNION ALL
    SELECT 'Cotopaxi' UNION ALL
    SELECT 'El Oro' UNION ALL
    SELECT 'Esmeraldas' UNION ALL
    SELECT 'Galápagos' UNION ALL
    SELECT 'Guayas' UNION ALL
    SELECT 'Imbabura' UNION ALL
    SELECT 'Loja' UNION ALL
    SELECT 'Los Ríos' UNION ALL
    SELECT 'Manabí' UNION ALL
    SELECT 'Morona Santiago' UNION ALL
    SELECT 'Napo' UNION ALL
    SELECT 'Orellana' UNION ALL
    SELECT 'Pastaza' UNION ALL
    SELECT 'Pichincha' UNION ALL
    SELECT 'Santa Elena' UNION ALL
    SELECT 'Santo Domingo de los Tsáchilas' UNION ALL
    SELECT 'Sucumbíos' UNION ALL
    SELECT 'Tungurahua' UNION ALL
    SELECT 'Zamora Chinchipe'
) t WHERE c.name = 'Ecuador';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Pichincha' AS state_name, 'Quito' AS name UNION ALL
    SELECT 'Guayas', 'Guayaquil' UNION ALL
    SELECT 'Azuay', 'Cuenca' UNION ALL
    SELECT 'Manabí', 'Portoviejo' UNION ALL
    SELECT 'Manabí', 'Manta' UNION ALL
    SELECT 'Tungurahua', 'Ambato' UNION ALL
    SELECT 'Loja', 'Loja' UNION ALL
    SELECT 'Esmeraldas', 'Esmeraldas' UNION ALL
    SELECT 'El Oro', 'Machala' UNION ALL
    SELECT 'Los Ríos', 'Babahoyo' UNION ALL
    SELECT 'Chimborazo', 'Riobamba' UNION ALL
    SELECT 'Imbabura', 'Ibarra' UNION ALL
    SELECT 'Cotopaxi', 'Latacunga' UNION ALL
    SELECT 'Bolívar', 'Guaranda' UNION ALL
    SELECT 'Cañar', 'Azogues' UNION ALL
    SELECT 'Carchi', 'Tulcán' UNION ALL
    SELECT 'Galápagos', 'Puerto Baquerizo Moreno' UNION ALL
    SELECT 'Morona Santiago', 'Macas' UNION ALL
    SELECT 'Napo', 'Tena' UNION ALL
    SELECT 'Orellana', 'Puerto Francisco de Orellana' UNION ALL
    SELECT 'Pastaza', 'Puyo' UNION ALL
    SELECT 'Santa Elena', 'Santa Elena' UNION ALL
    SELECT 'Santo Domingo de los Tsáchilas', 'Santo Domingo' UNION ALL
    SELECT 'Sucumbíos', 'Nueva Loja' UNION ALL
    SELECT 'Zamora Chinchipe', 'Zamora'
) t ON t.state_name = s.name
WHERE c.name = 'Ecuador';

-- ------------------------------------------------------------
-- El Salvador
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Ahuachapán' AS name UNION ALL
    SELECT 'Cabañas' UNION ALL
    SELECT 'Chalatenango' UNION ALL
    SELECT 'Cuscatlán' UNION ALL
    SELECT 'La Libertad' UNION ALL
    SELECT 'La Paz' UNION ALL
    SELECT 'La Unión' UNION ALL
    SELECT 'Morazán' UNION ALL
    SELECT 'San Miguel' UNION ALL
    SELECT 'San Salvador' UNION ALL
    SELECT 'San Vicente' UNION ALL
    SELECT 'Santa Ana' UNION ALL
    SELECT 'Sonsonate' UNION ALL
    SELECT 'Usulután'
) t WHERE c.name = 'El Salvador';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'San Salvador' AS state_name, 'San Salvador' AS name UNION ALL
    SELECT 'San Salvador', 'Soyapango' UNION ALL
    SELECT 'Santa Ana', 'Santa Ana' UNION ALL
    SELECT 'San Miguel', 'San Miguel' UNION ALL
    SELECT 'La Libertad', 'Santa Tecla' UNION ALL
    SELECT 'Sonsonate', 'Sonsonate' UNION ALL
    SELECT 'Ahuachapán', 'Ahuachapán' UNION ALL
    SELECT 'Cabañas', 'Sensuntepeque' UNION ALL
    SELECT 'Chalatenango', 'Chalatenango' UNION ALL
    SELECT 'Cuscatlán', 'Cojutepeque' UNION ALL
    SELECT 'La Paz', 'Zacatecoluca' UNION ALL
    SELECT 'La Unión', 'La Unión' UNION ALL
    SELECT 'Morazán', 'San Francisco Gotera' UNION ALL
    SELECT 'San Vicente', 'San Vicente' UNION ALL
    SELECT 'Usulután', 'Usulután'
) t ON t.state_name = s.name
WHERE c.name = 'El Salvador';

-- ------------------------------------------------------------
-- Grenada
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Saint Andrew' AS name UNION ALL
    SELECT 'Saint David' UNION ALL
    SELECT 'Saint George' UNION ALL
    SELECT 'Saint John' UNION ALL
    SELECT 'Saint Mark' UNION ALL
    SELECT 'Saint Patrick' UNION ALL
    SELECT 'Carriacou and Petite Martinique'
) t WHERE c.name = 'Grenada';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Saint George' AS state_name, 'Saint George''s' AS name UNION ALL
    SELECT 'Saint Andrew', 'Grenville' UNION ALL
    SELECT 'Saint David', 'Saint David''s' UNION ALL
    SELECT 'Saint John', 'Gouyave' UNION ALL
    SELECT 'Saint Mark', 'Victoria' UNION ALL
    SELECT 'Saint Patrick', 'Sauteurs' UNION ALL
    SELECT 'Carriacou and Petite Martinique', 'Hillsborough'
) t ON t.state_name = s.name
WHERE c.name = 'Grenada';

-- ------------------------------------------------------------
-- Guatemala
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Alta Verapaz' AS name UNION ALL
    SELECT 'Baja Verapaz' UNION ALL
    SELECT 'Chimaltenango' UNION ALL
    SELECT 'Chiquimula' UNION ALL
    SELECT 'El Progreso' UNION ALL
    SELECT 'Escuintla' UNION ALL
    SELECT 'Guatemala' UNION ALL
    SELECT 'Huehuetenango' UNION ALL
    SELECT 'Izabal' UNION ALL
    SELECT 'Jalapa' UNION ALL
    SELECT 'Jutiapa' UNION ALL
    SELECT 'Petén' UNION ALL
    SELECT 'Quetzaltenango' UNION ALL
    SELECT 'Quiché' UNION ALL
    SELECT 'Retalhuleu' UNION ALL
    SELECT 'Sacatepéquez' UNION ALL
    SELECT 'San Marcos' UNION ALL
    SELECT 'Santa Rosa' UNION ALL
    SELECT 'Sololá' UNION ALL
    SELECT 'Suchitepéquez' UNION ALL
    SELECT 'Totonicapán' UNION ALL
    SELECT 'Zacapa'
) t WHERE c.name = 'Guatemala';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Guatemala' AS state_name, 'Guatemala City' AS name UNION ALL
    SELECT 'Guatemala', 'Mixco' UNION ALL
    SELECT 'Guatemala', 'Villa Nueva' UNION ALL
    SELECT 'Quetzaltenango', 'Quetzaltenango' UNION ALL
    SELECT 'Escuintla', 'Escuintla' UNION ALL
    SELECT 'Sacatepéquez', 'Antigua Guatemala' UNION ALL
    SELECT 'Alta Verapaz', 'Cobán' UNION ALL
    SELECT 'Baja Verapaz', 'Salamá' UNION ALL
    SELECT 'Chimaltenango', 'Chimaltenango' UNION ALL
    SELECT 'Chiquimula', 'Chiquimula' UNION ALL
    SELECT 'El Progreso', 'Guastatoya' UNION ALL
    SELECT 'Huehuetenango', 'Huehuetenango' UNION ALL
    SELECT 'Izabal', 'Puerto Barrios' UNION ALL
    SELECT 'Jalapa', 'Jalapa' UNION ALL
    SELECT 'Jutiapa', 'Jutiapa' UNION ALL
    SELECT 'Petén', 'Flores' UNION ALL
    SELECT 'Quiché', 'Santa Cruz del Quiché' UNION ALL
    SELECT 'Retalhuleu', 'Retalhuleu' UNION ALL
    SELECT 'San Marcos', 'San Marcos' UNION ALL
    SELECT 'Santa Rosa', 'Cuilapa' UNION ALL
    SELECT 'Sololá', 'Sololá' UNION ALL
    SELECT 'Suchitepéquez', 'Mazatenango' UNION ALL
    SELECT 'Totonicapán', 'Totonicapán' UNION ALL
    SELECT 'Zacapa', 'Zacapa'
) t ON t.state_name = s.name
WHERE c.name = 'Guatemala';

-- ------------------------------------------------------------
-- Guyana
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Barima-Waini' AS name UNION ALL
    SELECT 'Cuyuni-Mazaruni' UNION ALL
    SELECT 'Demerara-Mahaica' UNION ALL
    SELECT 'East Berbice-Corentyne' UNION ALL
    SELECT 'Essequibo Islands-West Demerara' UNION ALL
    SELECT 'Mahaica-Berbice' UNION ALL
    SELECT 'Pomeroon-Supenaam' UNION ALL
    SELECT 'Potaro-Siparuni' UNION ALL
    SELECT 'Upper Demerara-Berbice' UNION ALL
    SELECT 'Upper Takutu-Upper Essequibo'
) t WHERE c.name = 'Guyana';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Demerara-Mahaica' AS state_name, 'Georgetown' AS name UNION ALL
    SELECT 'Barima-Waini', 'Mabaruma' UNION ALL
    SELECT 'Cuyuni-Mazaruni', 'Bartica' UNION ALL
    SELECT 'East Berbice-Corentyne', 'New Amsterdam' UNION ALL
    SELECT 'Essequibo Islands-West Demerara', 'Vreed-en-Hoop' UNION ALL
    SELECT 'Mahaica-Berbice', 'Fort Wellington' UNION ALL
    SELECT 'Pomeroon-Supenaam', 'Anna Regina' UNION ALL
    SELECT 'Potaro-Siparuni', 'Mahdia' UNION ALL
    SELECT 'Upper Demerara-Berbice', 'Linden' UNION ALL
    SELECT 'Upper Takutu-Upper Essequibo', 'Lethem'
) t ON t.state_name = s.name
WHERE c.name = 'Guyana';

-- ------------------------------------------------------------
-- Haiti
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Artibonite' AS name UNION ALL
    SELECT 'Centre' UNION ALL
    SELECT 'Grand''Anse' UNION ALL
    SELECT 'Nippes' UNION ALL
    SELECT 'Nord' UNION ALL
    SELECT 'Nord-Est' UNION ALL
    SELECT 'Nord-Ouest' UNION ALL
    SELECT 'Ouest' UNION ALL
    SELECT 'Sud' UNION ALL
    SELECT 'Sud-Est'
) t WHERE c.name = 'Haiti';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Ouest' AS state_name, 'Port-au-Prince' AS name UNION ALL
    SELECT 'Ouest', 'Carrefour' UNION ALL
    SELECT 'Ouest', 'Delmas' UNION ALL
    SELECT 'Artibonite', 'Gonaïves' UNION ALL
    SELECT 'Centre', 'Hinche' UNION ALL
    SELECT 'Grand''Anse', 'Jérémie' UNION ALL
    SELECT 'Nippes', 'Miragoâne' UNION ALL
    SELECT 'Nord', 'Cap-Haïtien' UNION ALL
    SELECT 'Nord-Est', 'Fort-Liberté' UNION ALL
    SELECT 'Nord-Ouest', 'Port-de-Paix' UNION ALL
    SELECT 'Sud', 'Les Cayes' UNION ALL
    SELECT 'Sud-Est', 'Jacmel'
) t ON t.state_name = s.name
WHERE c.name = 'Haiti';

-- ------------------------------------------------------------
-- Honduras
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Atlántida' AS name UNION ALL
    SELECT 'Choluteca' UNION ALL
    SELECT 'Colón' UNION ALL
    SELECT 'Comayagua' UNION ALL
    SELECT 'Copán' UNION ALL
    SELECT 'Cortés' UNION ALL
    SELECT 'El Paraíso' UNION ALL
    SELECT 'Francisco Morazán' UNION ALL
    SELECT 'Gracias a Dios' UNION ALL
    SELECT 'Intibucá' UNION ALL
    SELECT 'Islas de la Bahía' UNION ALL
    SELECT 'La Paz' UNION ALL
    SELECT 'Lempira' UNION ALL
    SELECT 'Ocotepeque' UNION ALL
    SELECT 'Olancho' UNION ALL
    SELECT 'Santa Bárbara' UNION ALL
    SELECT 'Valle' UNION ALL
    SELECT 'Yoro'
) t WHERE c.name = 'Honduras';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Francisco Morazán' AS state_name, 'Tegucigalpa' AS name UNION ALL
    SELECT 'Cortés', 'San Pedro Sula' UNION ALL
    SELECT 'Cortés', 'Choloma' UNION ALL
    SELECT 'Cortés', 'Puerto Cortés' UNION ALL
    SELECT 'Atlántida', 'La Ceiba' UNION ALL
    SELECT 'Choluteca', 'Choluteca' UNION ALL
    SELECT 'Colón', 'Trujillo' UNION ALL
    SELECT 'Comayagua', 'Comayagua' UNION ALL
    SELECT 'Copán', 'Santa Rosa de Copán' UNION ALL
    SELECT 'El Paraíso', 'Yuscarán' UNION ALL
    SELECT 'Gracias a Dios', 'Puerto Lempira' UNION ALL
    SELECT 'Intibucá', 'La Esperanza' UNION ALL
    SELECT 'Islas de la Bahía', 'Roatán' UNION ALL
    SELECT 'La Paz', 'La Paz' UNION ALL
    SELECT 'Lempira', 'Gracias' UNION ALL
    SELECT 'Ocotepeque', 'Nueva Ocotepeque' UNION ALL
    SELECT 'Olancho', 'Juticalpa' UNION ALL
    SELECT 'Santa Bárbara', 'Santa Bárbara' UNION ALL
    SELECT 'Valle', 'Nacaome' UNION ALL
    SELECT 'Yoro', 'Yoro' UNION ALL
    SELECT 'Yoro', 'El Progreso'
) t ON t.state_name = s.name
WHERE c.name = 'Honduras';

-- ------------------------------------------------------------
-- Jamaica
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Clarendon' AS name UNION ALL
    SELECT 'Hanover' UNION ALL
    SELECT 'Kingston' UNION ALL
    SELECT 'Manchester' UNION ALL
    SELECT 'Portland' UNION ALL
    SELECT 'Saint Andrew' UNION ALL
    SELECT 'Saint Ann' UNION ALL
    SELECT 'Saint Catherine' UNION ALL
    SELECT 'Saint Elizabeth' UNION ALL
    SELECT 'Saint James' UNION ALL
    SELECT 'Saint Mary' UNION ALL
    SELECT 'Saint Thomas' UNION ALL
    SELECT 'Trelawny' UNION ALL
    SELECT 'Westmoreland'
) t WHERE c.name = 'Jamaica';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Kingston' AS state_name, 'Kingston' AS name UNION ALL
    SELECT 'Saint Andrew', 'Half Way Tree' UNION ALL
    SELECT 'Saint Catherine', 'Spanish Town' UNION ALL
    SELECT 'Saint Catherine', 'Portmore' UNION ALL
    SELECT 'Saint James', 'Montego Bay' UNION ALL
    SELECT 'Manchester', 'Mandeville' UNION ALL
    SELECT 'Saint Ann', 'Saint Ann''s Bay' UNION ALL
    SELECT 'Clarendon', 'May Pen' UNION ALL
    SELECT 'Hanover', 'Lucea' UNION ALL
    SELECT 'Portland', 'Port Antonio' UNION ALL
    SELECT 'Saint Elizabeth', 'Black River' UNION ALL
    SELECT 'Saint Mary', 'Port Maria' UNION ALL
    SELECT 'Saint Thomas', 'Morant Bay' UNION ALL
    SELECT 'Trelawny', 'Falmouth' UNION ALL
    SELECT 'Westmoreland', 'Savanna-la-Mar'
) t ON t.state_name = s.name
WHERE c.name = 'Jamaica';

-- ------------------------------------------------------------
-- Mexico
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Aguascalientes' AS name UNION ALL
    SELECT 'Baja California' UNION ALL
    SELECT 'Baja California Sur' UNION ALL
    SELECT 'Campeche' UNION ALL
    SELECT 'Chiapas' UNION ALL
    SELECT 'Chihuahua' UNION ALL
    SELECT 'Ciudad de México' UNION ALL
    SELECT 'Coahuila' UNION ALL
    SELECT 'Colima' UNION ALL
    SELECT 'Durango' UNION ALL
    SELECT 'Estado de México' UNION ALL
    SELECT 'Guanajuato' UNION ALL
    SELECT 'Guerrero' UNION ALL
    SELECT 'Hidalgo' UNION ALL
    SELECT 'Jalisco' UNION ALL
    SELECT 'Michoacán' UNION ALL
    SELECT 'Morelos' UNION ALL
    SELECT 'Nayarit' UNION ALL
    SELECT 'Nuevo León' UNION ALL
    SELECT 'Oaxaca' UNION ALL
    SELECT 'Puebla' UNION ALL
    SELECT 'Querétaro' UNION ALL
    SELECT 'Quintana Roo' UNION ALL
    SELECT 'San Luis Potosí' UNION ALL
    SELECT 'Sinaloa' UNION ALL
    SELECT 'Sonora' UNION ALL
    SELECT 'Tabasco' UNION ALL
    SELECT 'Tamaulipas' UNION ALL
    SELECT 'Tlaxcala' UNION ALL
    SELECT 'Veracruz' UNION ALL
    SELECT 'Yucatán' UNION ALL
    SELECT 'Zacatecas'
) t WHERE c.name = 'Mexico';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Aguascalientes' AS state_name, 'Aguascalientes' AS name UNION ALL
    SELECT 'Baja California', 'Mexicali' UNION ALL
    SELECT 'Baja California', 'Tijuana' UNION ALL
    SELECT 'Baja California', 'Ensenada' UNION ALL
    SELECT 'Baja California Sur', 'La Paz' UNION ALL
    SELECT 'Baja California Sur', 'Cabo San Lucas' UNION ALL
    SELECT 'Campeche', 'Campeche' UNION ALL
    SELECT 'Chiapas', 'Tuxtla Gutiérrez' UNION ALL
    SELECT 'Chiapas', 'San Cristóbal de las Casas' UNION ALL
    SELECT 'Chihuahua', 'Chihuahua' UNION ALL
    SELECT 'Chihuahua', 'Ciudad Juárez' UNION ALL
    SELECT 'Ciudad de México', 'Ciudad de México' UNION ALL
    SELECT 'Coahuila', 'Saltillo' UNION ALL
    SELECT 'Coahuila', 'Torreón' UNION ALL
    SELECT 'Colima', 'Colima' UNION ALL
    SELECT 'Colima', 'Manzanillo' UNION ALL
    SELECT 'Durango', 'Durango' UNION ALL
    SELECT 'Estado de México', 'Toluca' UNION ALL
    SELECT 'Estado de México', 'Ecatepec' UNION ALL
    SELECT 'Estado de México', 'Naucalpan' UNION ALL
    SELECT 'Guanajuato', 'Guanajuato' UNION ALL
    SELECT 'Guanajuato', 'León' UNION ALL
    SELECT 'Guanajuato', 'Irapuato' UNION ALL
    SELECT 'Guerrero', 'Chilpancingo' UNION ALL
    SELECT 'Guerrero', 'Acapulco' UNION ALL
    SELECT 'Hidalgo', 'Pachuca' UNION ALL
    SELECT 'Jalisco', 'Guadalajara' UNION ALL
    SELECT 'Jalisco', 'Zapopan' UNION ALL
    SELECT 'Jalisco', 'Puerto Vallarta' UNION ALL
    SELECT 'Michoacán', 'Morelia' UNION ALL
    SELECT 'Michoacán', 'Uruapan' UNION ALL
    SELECT 'Morelos', 'Cuernavaca' UNION ALL
    SELECT 'Nayarit', 'Tepic' UNION ALL
    SELECT 'Nuevo León', 'Monterrey' UNION ALL
    SELECT 'Nuevo León', 'San Pedro Garza García' UNION ALL
    SELECT 'Oaxaca', 'Oaxaca' UNION ALL
    SELECT 'Puebla', 'Puebla' UNION ALL
    SELECT 'Querétaro', 'Querétaro' UNION ALL
    SELECT 'Quintana Roo', 'Chetumal' UNION ALL
    SELECT 'Quintana Roo', 'Cancún' UNION ALL
    SELECT 'Quintana Roo', 'Playa del Carmen' UNION ALL
    SELECT 'San Luis Potosí', 'San Luis Potosí' UNION ALL
    SELECT 'Sinaloa', 'Culiacán' UNION ALL
    SELECT 'Sinaloa', 'Mazatlán' UNION ALL
    SELECT 'Sonora', 'Hermosillo' UNION ALL
    SELECT 'Sonora', 'Ciudad Obregón' UNION ALL
    SELECT 'Tabasco', 'Villahermosa' UNION ALL
    SELECT 'Tamaulipas', 'Ciudad Victoria' UNION ALL
    SELECT 'Tamaulipas', 'Tampico' UNION ALL
    SELECT 'Tamaulipas', 'Reynosa' UNION ALL
    SELECT 'Tlaxcala', 'Tlaxcala' UNION ALL
    SELECT 'Veracruz', 'Xalapa' UNION ALL
    SELECT 'Veracruz', 'Veracruz' UNION ALL
    SELECT 'Yucatán', 'Mérida' UNION ALL
    SELECT 'Zacatecas', 'Zacatecas'
) t ON t.state_name = s.name
WHERE c.name = 'Mexico';

-- ------------------------------------------------------------
-- Nicaragua
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Boaco' AS name UNION ALL
    SELECT 'Carazo' UNION ALL
    SELECT 'Chinandega' UNION ALL
    SELECT 'Chontales' UNION ALL
    SELECT 'Costa Caribe Norte' UNION ALL
    SELECT 'Costa Caribe Sur' UNION ALL
    SELECT 'Estelí' UNION ALL
    SELECT 'Granada' UNION ALL
    SELECT 'Jinotega' UNION ALL
    SELECT 'León' UNION ALL
    SELECT 'Madriz' UNION ALL
    SELECT 'Managua' UNION ALL
    SELECT 'Masaya' UNION ALL
    SELECT 'Matagalpa' UNION ALL
    SELECT 'Nueva Segovia' UNION ALL
    SELECT 'Río San Juan' UNION ALL
    SELECT 'Rivas'
) t WHERE c.name = 'Nicaragua';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Managua' AS state_name, 'Managua' AS name UNION ALL
    SELECT 'León', 'León' UNION ALL
    SELECT 'Granada', 'Granada' UNION ALL
    SELECT 'Masaya', 'Masaya' UNION ALL
    SELECT 'Chinandega', 'Chinandega' UNION ALL
    SELECT 'Matagalpa', 'Matagalpa' UNION ALL
    SELECT 'Estelí', 'Estelí' UNION ALL
    SELECT 'Jinotega', 'Jinotega' UNION ALL
    SELECT 'Boaco', 'Boaco' UNION ALL
    SELECT 'Carazo', 'Jinotepe' UNION ALL
    SELECT 'Chontales', 'Juigalpa' UNION ALL
    SELECT 'Costa Caribe Norte', 'Bilwi' UNION ALL
    SELECT 'Costa Caribe Sur', 'Bluefields' UNION ALL
    SELECT 'Madriz', 'Somoto' UNION ALL
    SELECT 'Nueva Segovia', 'Ocotal' UNION ALL
    SELECT 'Río San Juan', 'San Carlos' UNION ALL
    SELECT 'Rivas', 'Rivas'
) t ON t.state_name = s.name
WHERE c.name = 'Nicaragua';

-- ------------------------------------------------------------
-- Panama
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Bocas del Toro' AS name UNION ALL
    SELECT 'Chiriquí' UNION ALL
    SELECT 'Coclé' UNION ALL
    SELECT 'Colón' UNION ALL
    SELECT 'Darién' UNION ALL
    SELECT 'Herrera' UNION ALL
    SELECT 'Los Santos' UNION ALL
    SELECT 'Panamá' UNION ALL
    SELECT 'Panamá Oeste' UNION ALL
    SELECT 'Veraguas' UNION ALL
    SELECT 'Emberá-Wounaan' UNION ALL
    SELECT 'Guna Yala' UNION ALL
    SELECT 'Ngäbe-Buglé'
) t WHERE c.name = 'Panama';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Panamá' AS state_name, 'Panama City' AS name UNION ALL
    SELECT 'Panamá Oeste', 'La Chorrera' UNION ALL
    SELECT 'Colón', 'Colón' UNION ALL
    SELECT 'Chiriquí', 'David' UNION ALL
    SELECT 'Bocas del Toro', 'Bocas del Toro' UNION ALL
    SELECT 'Coclé', 'Penonomé' UNION ALL
    SELECT 'Darién', 'La Palma' UNION ALL
    SELECT 'Herrera', 'Chitré' UNION ALL
    SELECT 'Los Santos', 'Las Tablas' UNION ALL
    SELECT 'Veraguas', 'Santiago de Veraguas' UNION ALL
    SELECT 'Emberá-Wounaan', 'Unión Chocó' UNION ALL
    SELECT 'Guna Yala', 'El Porvenir' UNION ALL
    SELECT 'Ngäbe-Buglé', 'Buäbti'
) t ON t.state_name = s.name
WHERE c.name = 'Panama';

-- ------------------------------------------------------------
-- Paraguay
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Alto Paraguay' AS name UNION ALL
    SELECT 'Alto Paraná' UNION ALL
    SELECT 'Amambay' UNION ALL
    SELECT 'Asunción' UNION ALL
    SELECT 'Boquerón' UNION ALL
    SELECT 'Caaguazú' UNION ALL
    SELECT 'Caazapá' UNION ALL
    SELECT 'Canindeyú' UNION ALL
    SELECT 'Central' UNION ALL
    SELECT 'Concepción' UNION ALL
    SELECT 'Cordillera' UNION ALL
    SELECT 'Guairá' UNION ALL
    SELECT 'Itapúa' UNION ALL
    SELECT 'Misiones' UNION ALL
    SELECT 'Ñeembucú' UNION ALL
    SELECT 'Paraguarí' UNION ALL
    SELECT 'Presidente Hayes' UNION ALL
    SELECT 'San Pedro'
) t WHERE c.name = 'Paraguay';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Asunción' AS state_name, 'Asunción' AS name UNION ALL
    SELECT 'Central', 'San Lorenzo' UNION ALL
    SELECT 'Central', 'Luque' UNION ALL
    SELECT 'Central', 'Lambaré' UNION ALL
    SELECT 'Alto Paraná', 'Ciudad del Este' UNION ALL
    SELECT 'Itapúa', 'Encarnación' UNION ALL
    SELECT 'Caaguazú', 'Coronel Oviedo' UNION ALL
    SELECT 'Concepción', 'Concepción' UNION ALL
    SELECT 'Amambay', 'Pedro Juan Caballero' UNION ALL
    SELECT 'Alto Paraguay', 'Fuerte Olimpo' UNION ALL
    SELECT 'Boquerón', 'Filadelfia' UNION ALL
    SELECT 'Caazapá', 'Caazapá' UNION ALL
    SELECT 'Canindeyú', 'Salto del Guairá' UNION ALL
    SELECT 'Cordillera', 'Caacupé' UNION ALL
    SELECT 'Guairá', 'Villarrica' UNION ALL
    SELECT 'Misiones', 'San Juan Bautista' UNION ALL
    SELECT 'Ñeembucú', 'Pilar' UNION ALL
    SELECT 'Paraguarí', 'Paraguarí' UNION ALL
    SELECT 'Presidente Hayes', 'Villa Hayes' UNION ALL
    SELECT 'San Pedro', 'San Pedro'
) t ON t.state_name = s.name
WHERE c.name = 'Paraguay';

-- ------------------------------------------------------------
-- Peru
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Amazonas' AS name UNION ALL
    SELECT 'Áncash' UNION ALL
    SELECT 'Apurímac' UNION ALL
    SELECT 'Arequipa' UNION ALL
    SELECT 'Ayacucho' UNION ALL
    SELECT 'Cajamarca' UNION ALL
    SELECT 'Callao' UNION ALL
    SELECT 'Cusco' UNION ALL
    SELECT 'Huancavelica' UNION ALL
    SELECT 'Huánuco' UNION ALL
    SELECT 'Ica' UNION ALL
    SELECT 'Junín' UNION ALL
    SELECT 'La Libertad' UNION ALL
    SELECT 'Lambayeque' UNION ALL
    SELECT 'Lima' UNION ALL
    SELECT 'Loreto' UNION ALL
    SELECT 'Madre de Dios' UNION ALL
    SELECT 'Moquegua' UNION ALL
    SELECT 'Pasco' UNION ALL
    SELECT 'Piura' UNION ALL
    SELECT 'Puno' UNION ALL
    SELECT 'San Martín' UNION ALL
    SELECT 'Tacna' UNION ALL
    SELECT 'Tumbes' UNION ALL
    SELECT 'Ucayali'
) t WHERE c.name = 'Peru';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Lima' AS state_name, 'Lima' AS name UNION ALL
    SELECT 'Callao', 'Callao' UNION ALL
    SELECT 'Arequipa', 'Arequipa' UNION ALL
    SELECT 'La Libertad', 'Trujillo' UNION ALL
    SELECT 'Lambayeque', 'Chiclayo' UNION ALL
    SELECT 'Piura', 'Piura' UNION ALL
    SELECT 'Cusco', 'Cusco' UNION ALL
    SELECT 'Junín', 'Huancayo' UNION ALL
    SELECT 'Loreto', 'Iquitos' UNION ALL
    SELECT 'Áncash', 'Huaraz' UNION ALL
    SELECT 'Áncash', 'Chimbote' UNION ALL
    SELECT 'Amazonas', 'Chachapoyas' UNION ALL
    SELECT 'Apurímac', 'Abancay' UNION ALL
    SELECT 'Ayacucho', 'Ayacucho' UNION ALL
    SELECT 'Cajamarca', 'Cajamarca' UNION ALL
    SELECT 'Huancavelica', 'Huancavelica' UNION ALL
    SELECT 'Huánuco', 'Huánuco' UNION ALL
    SELECT 'Ica', 'Ica' UNION ALL
    SELECT 'Madre de Dios', 'Puerto Maldonado' UNION ALL
    SELECT 'Moquegua', 'Moquegua' UNION ALL
    SELECT 'Pasco', 'Cerro de Pasco' UNION ALL
    SELECT 'Puno', 'Puno' UNION ALL
    SELECT 'San Martín', 'Moyobamba' UNION ALL
    SELECT 'San Martín', 'Tarapoto' UNION ALL
    SELECT 'Tacna', 'Tacna' UNION ALL
    SELECT 'Tumbes', 'Tumbes' UNION ALL
    SELECT 'Ucayali', 'Pucallpa'
) t ON t.state_name = s.name
WHERE c.name = 'Peru';

-- ------------------------------------------------------------
-- Saint Kitts and Nevis
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Christ Church Nichola Town' AS name UNION ALL
    SELECT 'Saint Anne Sandy Point' UNION ALL
    SELECT 'Saint George Basseterre' UNION ALL
    SELECT 'Saint George Gingerland' UNION ALL
    SELECT 'Saint James Windward' UNION ALL
    SELECT 'Saint John Capisterre' UNION ALL
    SELECT 'Saint John Figtree' UNION ALL
    SELECT 'Saint Mary Cayon' UNION ALL
    SELECT 'Saint Paul Capisterre' UNION ALL
    SELECT 'Saint Paul Charlestown' UNION ALL
    SELECT 'Saint Peter Basseterre' UNION ALL
    SELECT 'Saint Thomas Lowland' UNION ALL
    SELECT 'Saint Thomas Middle Island' UNION ALL
    SELECT 'Trinity Palmetto Point'
) t WHERE c.name = 'Saint Kitts and Nevis';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Saint George Basseterre' AS state_name, 'Basseterre' AS name UNION ALL
    SELECT 'Saint Paul Charlestown', 'Charlestown' UNION ALL
    SELECT 'Christ Church Nichola Town', 'Nichola Town' UNION ALL
    SELECT 'Saint Anne Sandy Point', 'Sandy Point Town' UNION ALL
    SELECT 'Saint Thomas Middle Island', 'Middle Island'
) t ON t.state_name = s.name
WHERE c.name = 'Saint Kitts and Nevis';

-- ------------------------------------------------------------
-- Saint Lucia
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Anse la Raye' AS name UNION ALL
    SELECT 'Castries' UNION ALL
    SELECT 'Choiseul' UNION ALL
    SELECT 'Dennery' UNION ALL
    SELECT 'Gros Islet' UNION ALL
    SELECT 'Laborie' UNION ALL
    SELECT 'Micoud' UNION ALL
    SELECT 'Soufrière' UNION ALL
    SELECT 'Vieux Fort' UNION ALL
    SELECT 'Canaries'
) t WHERE c.name = 'Saint Lucia';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Castries' AS state_name, 'Castries' AS name UNION ALL
    SELECT 'Gros Islet', 'Gros Islet' UNION ALL
    SELECT 'Soufrière', 'Soufrière' UNION ALL
    SELECT 'Vieux Fort', 'Vieux Fort' UNION ALL
    SELECT 'Micoud', 'Micoud' UNION ALL
    SELECT 'Dennery', 'Dennery' UNION ALL
    SELECT 'Laborie', 'Laborie' UNION ALL
    SELECT 'Choiseul', 'Choiseul' UNION ALL
    SELECT 'Anse la Raye', 'Anse la Raye' UNION ALL
    SELECT 'Canaries', 'Canaries'
) t ON t.state_name = s.name
WHERE c.name = 'Saint Lucia';

-- ------------------------------------------------------------
-- Saint Vincent and the Grenadines
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Charlotte' AS name UNION ALL
    SELECT 'Grenadines' UNION ALL
    SELECT 'Saint Andrew' UNION ALL
    SELECT 'Saint David' UNION ALL
    SELECT 'Saint George' UNION ALL
    SELECT 'Saint Patrick'
) t WHERE c.name = 'Saint Vincent and the Grenadines';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Saint George' AS state_name, 'Kingstown' AS name UNION ALL
    SELECT 'Charlotte', 'Georgetown' UNION ALL
    SELECT 'Grenadines', 'Port Elizabeth' UNION ALL
    SELECT 'Saint Andrew', 'Layou' UNION ALL
    SELECT 'Saint David', 'Chateaubelair' UNION ALL
    SELECT 'Saint Patrick', 'Barrouallie'
) t ON t.state_name = s.name
WHERE c.name = 'Saint Vincent and the Grenadines';

-- ------------------------------------------------------------
-- Suriname
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Brokopondo' AS name UNION ALL
    SELECT 'Commewijne' UNION ALL
    SELECT 'Coronie' UNION ALL
    SELECT 'Marowijne' UNION ALL
    SELECT 'Nickerie' UNION ALL
    SELECT 'Para' UNION ALL
    SELECT 'Paramaribo' UNION ALL
    SELECT 'Saramacca' UNION ALL
    SELECT 'Sipaliwini' UNION ALL
    SELECT 'Wanica'
) t WHERE c.name = 'Suriname';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Paramaribo' AS state_name, 'Paramaribo' AS name UNION ALL
    SELECT 'Wanica', 'Lelydorp' UNION ALL
    SELECT 'Nickerie', 'Nieuw Nickerie' UNION ALL
    SELECT 'Commewijne', 'Nieuw Amsterdam' UNION ALL
    SELECT 'Marowijne', 'Albina' UNION ALL
    SELECT 'Para', 'Onverwacht' UNION ALL
    SELECT 'Saramacca', 'Groningen' UNION ALL
    SELECT 'Brokopondo', 'Brokopondo' UNION ALL
    SELECT 'Coronie', 'Totness' UNION ALL
    SELECT 'Sipaliwini', 'Atjoni'
) t ON t.state_name = s.name
WHERE c.name = 'Suriname';

-- ------------------------------------------------------------
-- Trinidad and Tobago
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Arima' AS name UNION ALL
    SELECT 'Chaguanas' UNION ALL
    SELECT 'Couva-Tabaquite-Talparo' UNION ALL
    SELECT 'Diego Martin' UNION ALL
    SELECT 'Eastern Tobago' UNION ALL
    SELECT 'Mayaro-Rio Claro' UNION ALL
    SELECT 'Penal-Debe' UNION ALL
    SELECT 'Point Fortin' UNION ALL
    SELECT 'Port of Spain' UNION ALL
    SELECT 'Princes Town' UNION ALL
    SELECT 'San Fernando' UNION ALL
    SELECT 'San Juan-Laventille' UNION ALL
    SELECT 'Sangre Grande' UNION ALL
    SELECT 'Siparia' UNION ALL
    SELECT 'Tunapuna-Piarco' UNION ALL
    SELECT 'Western Tobago'
) t WHERE c.name = 'Trinidad and Tobago';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Port of Spain' AS state_name, 'Port of Spain' AS name UNION ALL
    SELECT 'San Fernando', 'San Fernando' UNION ALL
    SELECT 'Chaguanas', 'Chaguanas' UNION ALL
    SELECT 'Arima', 'Arima' UNION ALL
    SELECT 'Point Fortin', 'Point Fortin' UNION ALL
    SELECT 'Western Tobago', 'Scarborough' UNION ALL
    SELECT 'San Juan-Laventille', 'San Juan' UNION ALL
    SELECT 'Diego Martin', 'Diego Martin' UNION ALL
    SELECT 'Tunapuna-Piarco', 'Tunapuna' UNION ALL
    SELECT 'Sangre Grande', 'Sangre Grande' UNION ALL
    SELECT 'Couva-Tabaquite-Talparo', 'Couva' UNION ALL
    SELECT 'Penal-Debe', 'Penal' UNION ALL
    SELECT 'Princes Town', 'Princes Town' UNION ALL
    SELECT 'Siparia', 'Siparia' UNION ALL
    SELECT 'Mayaro-Rio Claro', 'Rio Claro' UNION ALL
    SELECT 'Eastern Tobago', 'Roxborough'
) t ON t.state_name = s.name
WHERE c.name = 'Trinidad and Tobago';

-- ------------------------------------------------------------
-- United States
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Alabama' AS name UNION ALL
    SELECT 'Alaska' UNION ALL
    SELECT 'Arizona' UNION ALL
    SELECT 'Arkansas' UNION ALL
    SELECT 'California' UNION ALL
    SELECT 'Colorado' UNION ALL
    SELECT 'Connecticut' UNION ALL
    SELECT 'Delaware' UNION ALL
    SELECT 'District of Columbia' UNION ALL
    SELECT 'Florida' UNION ALL
    SELECT 'Georgia' UNION ALL
    SELECT 'Hawaii' UNION ALL
    SELECT 'Idaho' UNION ALL
    SELECT 'Illinois' UNION ALL
    SELECT 'Indiana' UNION ALL
    SELECT 'Iowa' UNION ALL
    SELECT 'Kansas' UNION ALL
    SELECT 'Kentucky' UNION ALL
    SELECT 'Louisiana' UNION ALL
    SELECT 'Maine' UNION ALL
    SELECT 'Maryland' UNION ALL
    SELECT 'Massachusetts' UNION ALL
    SELECT 'Michigan' UNION ALL
    SELECT 'Minnesota' UNION ALL
    SELECT 'Mississippi' UNION ALL
    SELECT 'Missouri' UNION ALL
    SELECT 'Montana' UNION ALL
    SELECT 'Nebraska' UNION ALL
    SELECT 'Nevada' UNION ALL
    SELECT 'New Hampshire' UNION ALL
    SELECT 'New Jersey' UNION ALL
    SELECT 'New Mexico' UNION ALL
    SELECT 'New York' UNION ALL
    SELECT 'North Carolina' UNION ALL
    SELECT 'North Dakota' UNION ALL
    SELECT 'Ohio' UNION ALL
    SELECT 'Oklahoma' UNION ALL
    SELECT 'Oregon' UNION ALL
    SELECT 'Pennsylvania' UNION ALL
    SELECT 'Rhode Island' UNION ALL
    SELECT 'South Carolina' UNION ALL
    SELECT 'South Dakota' UNION ALL
    SELECT 'Tennessee' UNION ALL
    SELECT 'Texas' UNION ALL
    SELECT 'Utah' UNION ALL
    SELECT 'Vermont' UNION ALL
    SELECT 'Virginia' UNION ALL
    SELECT 'Washington' UNION ALL
    SELECT 'West Virginia' UNION ALL
    SELECT 'Wisconsin' UNION ALL
    SELECT 'Wyoming'
) t WHERE c.name = 'United States';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Alabama' AS state_name, 'Montgomery' AS name UNION ALL
    SELECT 'Alabama', 'Birmingham' UNION ALL
    SELECT 'Alabama', 'Huntsville' UNION ALL
    SELECT 'Alaska', 'Juneau' UNION ALL
    SELECT 'Alaska', 'Anchorage' UNION ALL
    SELECT 'Arizona', 'Phoenix' UNION ALL
    SELECT 'Arizona', 'Tucson' UNION ALL
    SELECT 'Arizona', 'Mesa' UNION ALL
    SELECT 'Arkansas', 'Little Rock' UNION ALL
    SELECT 'Arkansas', 'Fayetteville' UNION ALL
    SELECT 'California', 'Sacramento' UNION ALL
    SELECT 'California', 'Los Angeles' UNION ALL
    SELECT 'California', 'San Francisco' UNION ALL
    SELECT 'California', 'San Diego' UNION ALL
    SELECT 'California', 'San Jose' UNION ALL
    SELECT 'Colorado', 'Denver' UNION ALL
    SELECT 'Colorado', 'Colorado Springs' UNION ALL
    SELECT 'Connecticut', 'Hartford' UNION ALL
    SELECT 'Connecticut', 'Bridgeport' UNION ALL
    SELECT 'Delaware', 'Dover' UNION ALL
    SELECT 'Delaware', 'Wilmington' UNION ALL
    SELECT 'District of Columbia', 'Washington' UNION ALL
    SELECT 'Florida', 'Tallahassee' UNION ALL
    SELECT 'Florida', 'Miami' UNION ALL
    SELECT 'Florida', 'Orlando' UNION ALL
    SELECT 'Florida', 'Tampa' UNION ALL
    SELECT 'Florida', 'Jacksonville' UNION ALL
    SELECT 'Georgia', 'Atlanta' UNION ALL
    SELECT 'Georgia', 'Savannah' UNION ALL
    SELECT 'Hawaii', 'Honolulu' UNION ALL
    SELECT 'Idaho', 'Boise' UNION ALL
    SELECT 'Illinois', 'Springfield' UNION ALL
    SELECT 'Illinois', 'Chicago' UNION ALL
    SELECT 'Indiana', 'Indianapolis' UNION ALL
    SELECT 'Indiana', 'Fort Wayne' UNION ALL
    SELECT 'Iowa', 'Des Moines' UNION ALL
    SELECT 'Iowa', 'Cedar Rapids' UNION ALL
    SELECT 'Kansas', 'Topeka' UNION ALL
    SELECT 'Kansas', 'Wichita' UNION ALL
    SELECT 'Kentucky', 'Frankfort' UNION ALL
    SELECT 'Kentucky', 'Louisville' UNION ALL
    SELECT 'Kentucky', 'Lexington' UNION ALL
    SELECT 'Louisiana', 'Baton Rouge' UNION ALL
    SELECT 'Louisiana', 'New Orleans' UNION ALL
    SELECT 'Maine', 'Augusta' UNION ALL
    SELECT 'Maine', 'Portland' UNION ALL
    SELECT 'Maryland', 'Annapolis' UNION ALL
    SELECT 'Maryland', 'Baltimore' UNION ALL
    SELECT 'Massachusetts', 'Boston' UNION ALL
    SELECT 'Massachusetts', 'Worcester' UNION ALL
    SELECT 'Michigan', 'Lansing' UNION ALL
    SELECT 'Michigan', 'Detroit' UNION ALL
    SELECT 'Michigan', 'Grand Rapids' UNION ALL
    SELECT 'Minnesota', 'Saint Paul' UNION ALL
    SELECT 'Minnesota', 'Minneapolis' UNION ALL
    SELECT 'Mississippi', 'Jackson' UNION ALL
    SELECT 'Mississippi', 'Gulfport' UNION ALL
    SELECT 'Missouri', 'Jefferson City' UNION ALL
    SELECT 'Missouri', 'Kansas City' UNION ALL
    SELECT 'Missouri', 'St. Louis' UNION ALL
    SELECT 'Montana', 'Helena' UNION ALL
    SELECT 'Montana', 'Billings' UNION ALL
    SELECT 'Nebraska', 'Lincoln' UNION ALL
    SELECT 'Nebraska', 'Omaha' UNION ALL
    SELECT 'Nevada', 'Carson City' UNION ALL
    SELECT 'Nevada', 'Las Vegas' UNION ALL
    SELECT 'Nevada', 'Reno' UNION ALL
    SELECT 'New Hampshire', 'Concord' UNION ALL
    SELECT 'New Hampshire', 'Manchester' UNION ALL
    SELECT 'New Jersey', 'Trenton' UNION ALL
    SELECT 'New Jersey', 'Newark' UNION ALL
    SELECT 'New Jersey', 'Jersey City' UNION ALL
    SELECT 'New Mexico', 'Santa Fe' UNION ALL
    SELECT 'New Mexico', 'Albuquerque' UNION ALL
    SELECT 'New York', 'Albany' UNION ALL
    SELECT 'New York', 'New York City' UNION ALL
    SELECT 'New York', 'Buffalo' UNION ALL
    SELECT 'New York', 'Rochester' UNION ALL
    SELECT 'North Carolina', 'Raleigh' UNION ALL
    SELECT 'North Carolina', 'Charlotte' UNION ALL
    SELECT 'North Dakota', 'Bismarck' UNION ALL
    SELECT 'North Dakota', 'Fargo' UNION ALL
    SELECT 'Ohio', 'Columbus' UNION ALL
    SELECT 'Ohio', 'Cleveland' UNION ALL
    SELECT 'Ohio', 'Cincinnati' UNION ALL
    SELECT 'Oklahoma', 'Oklahoma City' UNION ALL
    SELECT 'Oklahoma', 'Tulsa' UNION ALL
    SELECT 'Oregon', 'Salem' UNION ALL
    SELECT 'Oregon', 'Portland' UNION ALL
    SELECT 'Oregon', 'Eugene' UNION ALL
    SELECT 'Pennsylvania', 'Harrisburg' UNION ALL
    SELECT 'Pennsylvania', 'Philadelphia' UNION ALL
    SELECT 'Pennsylvania', 'Pittsburgh' UNION ALL
    SELECT 'Rhode Island', 'Providence' UNION ALL
    SELECT 'South Carolina', 'Columbia' UNION ALL
    SELECT 'South Carolina', 'Charleston' UNION ALL
    SELECT 'South Dakota', 'Pierre' UNION ALL
    SELECT 'South Dakota', 'Sioux Falls' UNION ALL
    SELECT 'Tennessee', 'Nashville' UNION ALL
    SELECT 'Tennessee', 'Memphis' UNION ALL
    SELECT 'Tennessee', 'Knoxville' UNION ALL
    SELECT 'Texas', 'Austin' UNION ALL
    SELECT 'Texas', 'Houston' UNION ALL
    SELECT 'Texas', 'Dallas' UNION ALL
    SELECT 'Texas', 'San Antonio' UNION ALL
    SELECT 'Texas', 'Fort Worth' UNION ALL
    SELECT 'Utah', 'Salt Lake City' UNION ALL
    SELECT 'Utah', 'West Valley City' UNION ALL
    SELECT 'Vermont', 'Montpelier' UNION ALL
    SELECT 'Vermont', 'Burlington' UNION ALL
    SELECT 'Virginia', 'Richmond' UNION ALL
    SELECT 'Virginia', 'Virginia Beach' UNION ALL
    SELECT 'Virginia', 'Norfolk' UNION ALL
    SELECT 'Washington', 'Olympia' UNION ALL
    SELECT 'Washington', 'Seattle' UNION ALL
    SELECT 'Washington', 'Spokane' UNION ALL
    SELECT 'West Virginia', 'Charleston' UNION ALL
    SELECT 'Wisconsin', 'Madison' UNION ALL
    SELECT 'Wisconsin', 'Milwaukee' UNION ALL
    SELECT 'Wyoming', 'Cheyenne' UNION ALL
    SELECT 'Wyoming', 'Casper'
) t ON t.state_name = s.name
WHERE c.name = 'United States';

-- ------------------------------------------------------------
-- Uruguay
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Artigas' AS name UNION ALL
    SELECT 'Canelones' UNION ALL
    SELECT 'Cerro Largo' UNION ALL
    SELECT 'Colonia' UNION ALL
    SELECT 'Durazno' UNION ALL
    SELECT 'Flores' UNION ALL
    SELECT 'Florida' UNION ALL
    SELECT 'Lavalleja' UNION ALL
    SELECT 'Maldonado' UNION ALL
    SELECT 'Montevideo' UNION ALL
    SELECT 'Paysandú' UNION ALL
    SELECT 'Río Negro' UNION ALL
    SELECT 'Rivera' UNION ALL
    SELECT 'Rocha' UNION ALL
    SELECT 'Salto' UNION ALL
    SELECT 'San José' UNION ALL
    SELECT 'Soriano' UNION ALL
    SELECT 'Tacuarembó' UNION ALL
    SELECT 'Treinta y Tres'
) t WHERE c.name = 'Uruguay';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Montevideo' AS state_name, 'Montevideo' AS name UNION ALL
    SELECT 'Canelones', 'Canelones' UNION ALL
    SELECT 'Canelones', 'Las Piedras' UNION ALL
    SELECT 'Maldonado', 'Maldonado' UNION ALL
    SELECT 'Maldonado', 'Punta del Este' UNION ALL
    SELECT 'Salto', 'Salto' UNION ALL
    SELECT 'Paysandú', 'Paysandú' UNION ALL
    SELECT 'Rivera', 'Rivera' UNION ALL
    SELECT 'Tacuarembó', 'Tacuarembó' UNION ALL
    SELECT 'Artigas', 'Artigas' UNION ALL
    SELECT 'Cerro Largo', 'Melo' UNION ALL
    SELECT 'Colonia', 'Colonia del Sacramento' UNION ALL
    SELECT 'Durazno', 'Durazno' UNION ALL
    SELECT 'Flores', 'Trinidad' UNION ALL
    SELECT 'Florida', 'Florida' UNION ALL
    SELECT 'Lavalleja', 'Minas' UNION ALL
    SELECT 'Río Negro', 'Fray Bentos' UNION ALL
    SELECT 'Rocha', 'Rocha' UNION ALL
    SELECT 'San José', 'San José de Mayo' UNION ALL
    SELECT 'Soriano', 'Mercedes' UNION ALL
    SELECT 'Treinta y Tres', 'Treinta y Tres'
) t ON t.state_name = s.name
WHERE c.name = 'Uruguay';

-- ------------------------------------------------------------
-- Venezuela
-- ------------------------------------------------------------
INSERT IGNORE INTO states (country_id, name)
SELECT c.id, t.name FROM countries c CROSS JOIN (
    SELECT 'Amazonas' AS name UNION ALL
    SELECT 'Anzoátegui' UNION ALL
    SELECT 'Apure' UNION ALL
    SELECT 'Aragua' UNION ALL
    SELECT 'Barinas' UNION ALL
    SELECT 'Bolívar' UNION ALL
    SELECT 'Carabobo' UNION ALL
    SELECT 'Cojedes' UNION ALL
    SELECT 'Delta Amacuro' UNION ALL
    SELECT 'Distrito Capital' UNION ALL
    SELECT 'Falcón' UNION ALL
    SELECT 'Guárico' UNION ALL
    SELECT 'La Guaira' UNION ALL
    SELECT 'Lara' UNION ALL
    SELECT 'Mérida' UNION ALL
    SELECT 'Miranda' UNION ALL
    SELECT 'Monagas' UNION ALL
    SELECT 'Nueva Esparta' UNION ALL
    SELECT 'Portuguesa' UNION ALL
    SELECT 'Sucre' UNION ALL
    SELECT 'Táchira' UNION ALL
    SELECT 'Trujillo' UNION ALL
    SELECT 'Yaracuy' UNION ALL
    SELECT 'Zulia'
) t WHERE c.name = 'Venezuela';

INSERT IGNORE INTO cities (state_id, name)
SELECT s.id, t.name FROM states s
INNER JOIN countries c ON s.country_id = c.id
INNER JOIN (
    SELECT 'Distrito Capital' AS state_name, 'Caracas' AS name UNION ALL
    SELECT 'Miranda', 'Los Teques' UNION ALL
    SELECT 'Miranda', 'Petare' UNION ALL
    SELECT 'Zulia', 'Maracaibo' UNION ALL
    SELECT 'Zulia', 'Cabimas' UNION ALL
    SELECT 'Carabobo', 'Valencia' UNION ALL
    SELECT 'Carabobo', 'Puerto Cabello' UNION ALL
    SELECT 'Aragua', 'Maracay' UNION ALL
    SELECT 'Lara', 'Barquisimeto' UNION ALL
    SELECT 'Bolívar', 'Ciudad Bolívar' UNION ALL
    SELECT 'Bolívar', 'Ciudad Guayana' UNION ALL
    SELECT 'Anzoátegui', 'Barcelona' UNION ALL
    SELECT 'Anzoátegui', 'Puerto La Cruz' UNION ALL
    SELECT 'Mérida', 'Mérida' UNION ALL
    SELECT 'Táchira', 'San Cristóbal' UNION ALL
    SELECT 'Sucre', 'Cumaná' UNION ALL
    SELECT 'Falcón', 'Coro' UNION ALL
    SELECT 'Falcón', 'Punto Fijo' UNION ALL
    SELECT 'Monagas', 'Maturín' UNION ALL
    SELECT 'Nueva Esparta', 'La Asunción' UNION ALL
    SELECT 'Nueva Esparta', 'Porlamar' UNION ALL
    SELECT 'Portuguesa', 'Guanare' UNION ALL
    SELECT 'Trujillo', 'Trujillo' UNION ALL
    SELECT 'Yaracuy', 'San Felipe' UNION ALL
    SELECT 'Amazonas', 'Puerto Ayacucho' UNION ALL
    SELECT 'Apure', 'San Fernando de Apure' UNION ALL
    SELECT 'Barinas', 'Barinas' UNION ALL
    SELECT 'Cojedes', 'San Carlos' UNION ALL
    SELECT 'Delta Amacuro', 'Tucupita' UNION ALL
    SELECT 'Guárico', 'San Juan de los Morros' UNION ALL
    SELECT 'La Guaira', 'La Guaira'
) t ON t.state_name = s.name
WHERE c.name = 'Venezuela';
