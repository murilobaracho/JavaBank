CREATE TABLE agencia ( 
    id serial primary key, 
    codigo varchar(10) unique not null, 
    nome varchar(100) not null, 
    endereco varchar(150), 
    telefone varchar(20) 
); 

CREATE TABLE cliente ( 
    id serial primary key, 
    nome varchar(100) not null, 
    cpf bigint unique not null, 
    data_nasc date, 
    telefone varchar(20), 
    email varchar(100) unique 
); 

CREATE TABLE conta ( 
    id serial primary key, 
    numero varchar(20) unique not null, 
    agencia_id integer references agencia(id), 
    cliente_id integer references cliente(id),
    tipo varchar(20) check (tipo IN ('corrente', 'poupanca')),
    saldo numeric(12,2) default 0 check (saldo >= 0),
    data_abertura timestamp 
); 

CREATE TABLE transacao ( 
    id serial primary key, 
    conta_id integer references conta(id),
    tipo varchar(20) check (tipo IN ('deposito', 'saque', 'pix', 'transferencia')), 
    valor numeric(12,2) check (valor > 0),
    data_hora timestamp default now(), 
    conta_destino_id integer references conta(id) null 
); 

CREATE TABLE emprestimo ( 
    id serial primary key, 
    cliente_id integer references cliente(id), 
    agencia_id integer references agencia(id), 
    valor numeric(12,2) check (valor > 0), 
    taxa_juros numeric(5,2) check (taxa_juros > 0 and taxa_juros < 20), 
    parcelas integer check (parcelas > 0), 
    status varchar(20) default 'pendente' check (status IN ('pendente', 'aprovado', 'quitado')) 
);

--inserts com Ctes para melhor validação/eficiencia

INSERT INTO "agencia" ("codigo", "nome", "endereco", "telefone") VALUES
('0001', 'Agência Central', 'Av. Paulista, 1000 - São Paulo/SP', '(11) 3333-1111'),
('0002', 'Agência Inovação', 'Rua das Flores, 50 - Curitiba/PR', '(41) 3222-2222');

INSERT INTO "cliente" ("nome", "cpf", "data_nasc", "telefone", "email") VALUES
('Ana Silva', 12345678901, '1990-05-15', '(11) 99999-1111', 'ana.silva@email.com'),
('Bruno Costa', 98765432100, '1985-10-22', '(21) 98888-2222', 'bruno.costa@email.com'),
('Carlos Souza', 45678912344, '1993-02-28', '(41) 97777-3333', 'carlos.souza@email.com');

INSERT INTO "conta" ("numero", "agencia_id", "cliente_id", "tipo", "saldo", "data_abertura") VALUES
('10001-X', (SELECT "id" FROM "agencia" WHERE "codigo" = '0001'), (SELECT "id" FROM "cliente" WHERE "cpf" = 12345678901), 'corrente', 1500.00, '2025-01-10 10:00:00'),
('10002-Y', (SELECT "id" FROM "agencia" WHERE "codigo" = '0001'), (SELECT "id" FROM "cliente" WHERE "cpf" = 12345678901), 'poupanca', 5000.00, '2025-01-12 14:30:00'),
('20001-A', (SELECT "id" FROM "agencia" WHERE "codigo" = '0002'), (SELECT "id" FROM "cliente" WHERE "cpf" = 98765432100), 'corrente', 250.50, '2025-02-01 09:15:00'),
('20002-B', (SELECT "id" FROM "agencia" WHERE "codigo" = '0002'), (SELECT "id" FROM "cliente" WHERE "cpf" = 45678912344), 'corrente', 3000.00, '2025-02-15 11:45:00');

INSERT INTO "emprestimo" ("cliente_id", "agencia_id", "valor", "taxa_juros", "parcelas", "status") VALUES
((SELECT "id" FROM "cliente" WHERE "cpf" = 12345678901), (SELECT "id" FROM "agencia" WHERE "codigo" = '0001'), 10000.00, 4.50, 24, 'aprovado'),
((SELECT "id" FROM "cliente" WHERE "cpf" = 98765432100), (SELECT "id" FROM "agencia" WHERE "codigo" = '0002'), 5000.00, 5.20, 12, 'pendente');

INSERT INTO "transacao" ("conta_id", "tipo", "valor", "data_hora", "conta_destino_id") VALUES
((SELECT "id" FROM "conta" WHERE "numero" = '10001-X'), 'deposito', 500.00, '2026-03-01 10:30:00', NULL),
((SELECT "id" FROM "conta" WHERE "numero" = '10001-X'), 'saque', 100.00, '2026-03-02 15:20:00', NULL),
((SELECT "id" FROM "conta" WHERE "numero" = '20002-B'), 'pix', 350.00, '2026-03-03 18:00:00', (SELECT "id" FROM "conta" WHERE "numero" = '10001-X')),
((SELECT "id" FROM "conta" WHERE "numero" = '20001-A'), 'transferencia', 150.00, '2026-03-04 09:00:00', (SELECT "id" FROM "conta" WHERE "numero" = '10002-Y'));
