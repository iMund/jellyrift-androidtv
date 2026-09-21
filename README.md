<h1 align="center">JellyRift</h1>
<h3 align="center">Cliente Jellyfin para Android TV e Fire TV, baseado no app oficial</h3>

---

JellyRift é um fork do [Jellyfin for Android TV](https://github.com/jellyfin/jellyfin-androidtv). Eu mantenho porque precisava de algumas
correções que o app oficial não tem, principalmente para usar o Jellyfin com plugins que conversam com a TV. Continua sendo um cliente
Jellyfin comum: você entra no seu servidor, navega pela biblioteca e assiste como sempre.

Não é um projeto oficial do Jellyfin e não tem ligação com a equipe deles. "Jellyfin" é marca do projeto Jellyfin.

## O que muda em relação ao app oficial

- **Reprodução com servidor que exige login no streaming.** O ExoPlayer baixa o vídeo por conta própria e, no app oficial, faz isso sem enviar
  credencial nenhuma. Se o servidor, um plugin ou um proxy reverso exige autenticação para `/Videos/{id}/stream`, a resposta é 403 e o app
  cai para transcodificação. Aqui o cabeçalho de autorização vai junto, só para o servidor em que você entrou (mesmo esquema, host e porta).
- **`DisplayContent` que interrompe o vídeo.** O app oficial ignora `DisplayContent` enquanto algo toca, para que navegar em outro aparelho
  não pare o filme. O servidor não informa quem mandou o comando, então um plugin nunca conseguia mostrar nada em uma TV que está
  reproduzindo. Agora existe o argumento opcional `InterruptPlayback=true`: com ele o app para o vídeo e abre o conteúdo. Sem o argumento,
  o comportamento é o de sempre.
- **`DisplayContent` repetido não empilha telas.** Se o mesmo item já está aberto, o comando é ignorado, e o botão voltar sai da página em vez
  de andar por cópias dela.
- **`DisplayMessage` respeita o tempo pedido** (`TimeoutMs`, entre 1 e 30 segundos) e mostra o título em uma linha separada do texto.
- **Correções no PlayNow remoto.** Um segundo PlayNow depois de uma reprodução derrubava o app com `NullPointerException`, e um PlayNow
  sobre um vídeo em andamento fechava o player novo junto com o antigo.
- **Voltar na tela inicial pergunta se você quer sair**, e sair encerra o app de verdade, fechando a conexão com o servidor.
- **Atualização dentro do app**, a partir das releases deste repositório (veja abaixo).

Fora isso o app é o oficial. Da versão 1.1 em diante ele se identifica ao servidor como "JellyRift" (nas versões 1.0.x o nome era o do app
oficial, "Jellyfin for Android TV"). Plugins que reconhecem o cliente pelo nome precisam aceitar os dois.

## Instalação

Baixe o APK da [última release](https://github.com/iMund/jellyrift-androidtv/releases/latest). O app usa outro identificador
(`br.com.jellyrift.androidtv`), então instala ao lado do Jellyfin oficial sem substituí-lo, e você entra no servidor de novo.

- **Android TV:** copie o APK para a TV (pendrive, `adb install` ou um app de arquivos) e abra. Pode ser preciso liberar a instalação de
  apps de fontes desconhecidas para o aplicativo que você usou para abrir o arquivo.
- **Fire TV / Fire Stick:** instale o app Downloader, libere para ele a instalação de apps de fontes desconhecidas (o menu fica em Minha
  Fire TV, Opções do desenvolvedor, e varia conforme a versão do Fire OS), abra nele o endereço do APK da release e instale.

### Atualizando

Em Configurações, Sobre, "Buscar atualização". O app consulta a última release deste repositório, baixa o APK, confere o tamanho e o
SHA-256 publicados e entrega ao instalador do sistema, que pede a sua confirmação. O Android só aceita o APK se ele for assinado com a mesma
chave do app instalado. Na primeira vez o sistema pede para permitir que o JellyRift instale aplicativos.

## Compilando

Precisa do JDK 21 e do Android SDK. Com Android Studio já vem tudo. Sem ele, use o Gradle wrapper:

```shell
./gradlew assembleDebug
```

O APK sai em `app/build/outputs/apk/debug`. A versão de debug usa o identificador `br.com.jellyrift.androidtv.debug`, então convive com a
release.

Para gerar uma release assinada, informe o keystore por propriedades do Gradle (em `~/.gradle/gradle.properties`, nunca no repositório) ou
por variáveis de ambiente:

```properties
keystore.file=/caminho/para/jellyrift.jks
keystore.password=...
signing.key.alias=...
signing.key.password=...
```

```shell
./gradlew assembleRelease -Pjellyfin.version=1.0.0 -Pupdate.repository=iMund/jellyrift-androidtv
```

- `jellyfin.version` define a versão do app. O formato é `1.2.3` ou `1.2.3-rc.1`, e a atualização dentro do app compara a tag da release com
  essa versão do mesmo jeito que o build calcula o `versionCode`.
- `update.repository` é o repositório (`dono/nome`) onde o app procura atualizações. Vazio, o botão de atualização some.
- `update.api.url` troca o endereço da API do GitHub, útil para testar com um servidor local.

A tag da release no GitHub precisa ser a mesma versão (`v1.0.0`) e o APK precisa estar anexado a ela.

## Base e licença

O código vem do `jellyfin/jellyfin-androidtv` (branch `master`, a partir do commit `0d9400d0a`), e o histórico dele foi mantido. Como ele, o
JellyRift é distribuído sob a [GPL-2.0](LICENSE). Os avisos de copyright e as licenças das bibliotecas usadas aparecem em Configurações,
Sobre, Licenças.

Se a sua dúvida é sobre o Jellyfin em si, o lugar certo é o [projeto oficial](https://jellyfin.org). Problemas específicos do JellyRift
podem ser abertos como [issue aqui](https://github.com/iMund/jellyrift-androidtv/issues).
