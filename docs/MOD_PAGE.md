# Mod page copy

Paste-ready description for CurseForge and similar sites. English first, Turkish below.

---

## English

### Custom Jukebox Discs

**Put your own music on a real jukebox.**

Drop an MP3 or OGG file into a folder, write it onto a disc in the Disc Writer, and play it in any
vanilla jukebox. It sounds like a record should: positional, fading with distance, on the same volume
slider as every other record.

### How it works

1. Put your `.mp3` or `.ogg` files in `.minecraft/customjukeboxdiscs/uploads`
2. Craft a **Disc Writer** and a **Blank Disc**
3. Open the writer, pick your track, name it, press Write
4. Put the finished disc in a jukebox

Discs behave exactly like vanilla records: hoppers move them, comparators read them, breaking the
jukebox stops the music.

### Built for servers

The server is in charge of everything. It re-checks every upload, works out the real duration itself,
and never trusts what a client claims.

- **Permissions**: uploading is limited to operators by default. Switch to an allowlist or open it to
  everyone with `/customdiscs access mode`. A deny entry beats everything.
- **Quotas**: per-file size, track length, tracks per player, storage per player, storage per server.
- **Moderation**: `/customdiscs tracks list`, `info` and `delete` to audit and remove what people wrote.
- **Storage**: tracks live in the world folder, addressed by content hash, so the same song uploaded
  twice is stored once.
- **Bandwidth**: a player downloads a track once and it is cached for that server. Repeat plays are free.

### Not everyone wants to hear it

Any player can press **J** to switch custom music off for themselves. When it is off, the server
stops sending them tracks entirely, so it costs them no bandwidth at all.

### More than a disc

- **Twelve disc designs**, picked at random when a disc is written, so a shelf of music is easy to read
- **Disc Rack**: a nine slot shelf that shows the discs it holds. Right-click a disc to take it, sneak
  to open it like a chest
- **Sophisticated Backpacks / Storage**: works with their jukebox upgrade, music included. Music from a
  backpack you carry travels with you

### Writing from a link

Operators can allow writing a track from a direct `https` link to an audio file, from hosts they
choose. The server downloads and validates it exactly like an upload. Off by default.

**This is not a streaming mod.** It cannot play from YouTube, Spotify or similar services, and that is
not planned. The link has to point at an actual audio file: your own web space, the Internet Archive,
the Free Music Archive, a file you uploaded to Discord.

### Requirements

- Minecraft 1.21.1, NeoForge 21.1.231 or newer
- Required on both the client and the server
- No FFmpeg, no native binaries, no telemetry, nothing phoning home
- MIT licensed, so modpacks are welcome to include it

### A word on what you upload

Only upload audio you have the right to share. Server operators decide who may write discs and can
delete anything at any time.

---

## Türkçe

### Custom Jukebox Discs

**Kendi müziğini gerçek bir jukebox'ta çal.**

Bir MP3 ya da OGG dosyasını klasöre at, Disk Yazıcı ile diske yaz, herhangi bir vanilla jukebox'a tak.
Plak gibi duyulur: konumsal, uzaklaştıkça kısılan, diğer plaklarla aynı ses ayarına bağlı.

### Nasıl çalışır

1. `.mp3` veya `.ogg` dosyalarını `.minecraft/customjukeboxdiscs/uploads` klasörüne koy
2. **Disk Yazıcı** ve **Boş Disk** üret
3. Yazıcıyı aç, parçanı seç, adını ver, Yaz'a bas
4. Çıkan diski jukebox'a tak

Diskler vanilla plaklarla aynı davranır: hunilerle taşınır, komparatör okur, jukebox kırılınca müzik durur.

### Sunucular için tasarlandı

Her şeyin kararını sunucu verir. Yüklenen dosyayı baştan doğrular, süreyi kendisi hesaplar, istemcinin
söylediği hiçbir şeye güvenmez.

- **İzinler**: yükleme varsayılan olarak yetkililere açık. `/customdiscs access mode` ile izin listesine
  geçebilir ya da herkese açabilirsin. Yasaklama her şeyi ezer.
- **Kotalar**: dosya boyutu, parça süresi, oyuncu başına parça ve depolama, sunucu toplamı.
- **Denetim**: `/customdiscs tracks list`, `info`, `delete` ile yazılanları görebilir ve silebilirsin.
- **Depolama**: parçalar dünya klasöründe içerik hash'iyle saklanır; aynı şarkı iki kez yüklenirse yer
  bir kez kaplar.
- **Bant genişliği**: oyuncu bir parçayı bir kez indirir, o sunucu için önbelleğe alınır.

### Herkes dinlemek zorunda değil

Her oyuncu **J** ile özel müziği kendine kapatabilir. Kapalıyken sunucu ona parça göndermeyi tamamen
bırakır — yani hiç internet harcamaz.

### Sadece disk değil

- **On iki disk tasarımı**, yazarken rastgele seçilir; raftaki müzikleri ayırt etmek kolay olur
- **Disk Rafı**: dokuz gözlü, içindeki diskleri dışarıdan gösteren raf. Diske sağ tık alır, eğilip sağ
  tık sandık gibi açar
- **Sophisticated Backpacks / Storage**: jukebox upgrade'iyle çalışır. Sırtındaki çantanın müziği
  seninle gelir

### Linkten yazma

Sunucu yöneticisi, seçtiği host'lardan doğrudan `https` ses dosyası linkiyle disk yazmaya izin
verebilir. Sunucu dosyayı indirip yüklemeyle aynı doğrulamadan geçirir. Varsayılan olarak kapalı.

**Bu bir streaming modu değil.** YouTube, Spotify gibi servislerden çalamaz, çalması da planlanmıyor.
Link gerçek bir ses dosyasına işaret etmeli: kendi web alanın, Internet Archive, Free Music Archive,
Discord'a attığın bir dosya.

### Gereksinimler

- Minecraft 1.21.1, NeoForge 21.1.231 veya üzeri
- Hem istemcide hem sunucuda gerekli
- FFmpeg yok, native binary yok, telemetri yok
- MIT lisanslı, modpack'lere serbestçe eklenebilir

### Yüklediklerin hakkında

Sadece paylaşma hakkına sahip olduğun sesi yükle. Kimin disk yazabileceğine sunucu yöneticisi karar
verir ve istediği anda silebilir.
