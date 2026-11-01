Scriptum - dados mock para desenvolvimento local

Credenciais de teste:
  Usuário: admin
  Senha: scriptum123

Credencial alternativa:
  Usuário: ana
  Senha: 123456

categories.json e documents.json formam o catálogo remoto simulado. users.json
define as credenciais aceitas. tasks.json começa vazio e documenta o formato dos
registros enviados. Durante a execução, categorias, documentos e tarefas criados
pela tela são persistidos no armazenamento privado do aplicativo em files/mock,
sem modificar estes arquivos de origem.
