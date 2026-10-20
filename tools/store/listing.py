"""Google Play store listing texts, one entry per Play language. Writes store/listing/<locale>/*.txt and the
512px icon as the 32-bit PNG Play asks for (the fastlane "supply" layout that
.github/workflows/play-listing.yml uploads; tools/store/screenshots.mjs draws the screenshots):

    python3 tools/store/listing.py store/listing

Limits: title 30, short description 80, full description 4000 characters (checked).
"""
import os
import sys

from PIL import Image

# Shared by every language: the rate source credit the free API asks for.
CREDIT = "Rates By Exchange Rate API (https://www.exchangerate-api.com)"

LISTINGS = {
    "en-US": dict(
        title="Currency Converter + Widget",
        short="Convert 160+ currencies offline, from any app, with a home-screen widget.",
        full="""Convert any currency in a tap, even offline. Type an amount and see it in your currency right away, with rates for 160+ currencies that update by themselves.

★ Simple and fast
• A big, clear keypad: the result changes as you type
• Swap currencies with one tap, or add a third currency to convert to two at once
• Find a currency by country, name, symbol or code, in any language
• Works offline with the last rates you downloaded

★ Convert from any app
Select a price in your browser, a booking app or a chat and choose "Convert currency" (or Share → "Convert currency"). The result pops up over the app you are in. It understands $, €, ₪, "1.250,00", "1.5k" and more.

★ History
Every amount and calculation is kept. Tap one to bring it back with its currencies.

★ Premium
• Home-screen widget: type an amount and see it converted without opening the app
• Calculator with percentages: 120 + 15%, 3 × 800 and more, converted as you type
• 6 color themes, each in light and dark

New users get Premium free for 3 days. After that, watch 2 short videos to open it for 24 hours, or get Premium forever with one payment, which also removes the ads. Converting, search, the extra currency, history and converting from other apps always stay free.

★ 13 languages
English, Hebrew, Arabic, Spanish, Russian, French, German, Portuguese, Italian, Turkish, Chinese, Japanese and Hindi.

Rates are for information only and update about once a day. Your bank or exchange office may use a different rate.
""" + CREDIT),
    "iw-IL": dict(
        title="ממיר מטבעות + וידג'ט",
        short="המרת 160+ מטבעות, גם בלי אינטרנט, מכל אפליקציה ועם וידג'ט למסך הבית.",
        full="""המרת מטבע בנגיעה, גם בלי אינטרנט. מקלידים סכום ורואים אותו מיד במטבע שלכם, עם שערים של יותר מ־160 מטבעות שמתעדכנים לבד.

★ פשוט ומהיר
• מקלדת גדולה וברורה: התוצאה משתנה תוך כדי הקלדה
• החלפת מטבעות בנגיעה, או מטבע שלישי להמרה לשניים בבת אחת
• חיפוש מטבע לפי מדינה, שם, סמל או קוד, בכל שפה
• עובד גם בלי אינטרנט, עם השערים האחרונים שירדו

★ המרה מכל אפליקציה
מסמנים מחיר בדפדפן, באפליקציית הזמנות או בצ'אט ובוחרים "המרת מטבע" (או "שיתוף" ← "המרת מטבע"). התוצאה קופצת מעל האפליקציה שבה אתם נמצאים. מזהה $, €, ₪, "1.250,00", "1.5k" ועוד.

★ היסטוריה
כל סכום וכל חישוב נשמרים. נגיעה בשורה מחזירה אותה עם המטבעות שלה.

★ פרימיום
• וידג'ט למסך הבית: מקלידים סכום ורואים את ההמרה בלי לפתוח את האפליקציה
• מחשבון עם אחוזים: ‎120 + 15%‎, ‏‎3 × 800‎ ועוד, עם המרה תוך כדי הקלדה
• 6 ערכות צבעים, כל אחת בבהיר ובכהה

משתמשים חדשים מקבלים פרימיום חינם ל־3 ימים. אחר כך צפייה ב־2 סרטונים קצרים פותחת אותו ל־24 שעות, או פרימיום לתמיד בתשלום אחד, שגם מוריד את הפרסומות. ההמרה, החיפוש, המטבע הנוסף, ההיסטוריה וההמרה מאפליקציות אחרות תמיד נשארים בחינם.

★ 13 שפות
עברית, אנגלית, ערבית, ספרדית, רוסית, צרפתית, גרמנית, פורטוגזית, איטלקית, טורקית, סינית, יפנית והינדי.

השערים לידיעה בלבד ומתעדכנים בערך פעם ביום. בבנק או בצ'יינג' השער עשוי להיות שונה.
""" + CREDIT),
    "es-ES": dict(
        title="Conversor de Divisas + Widget",
        short="Convierte más de 160 monedas sin conexión, desde cualquier app y con widget.",
        full="""Convierte cualquier moneda con un toque, incluso sin conexión. Escribe un importe y míralo al instante en tu moneda, con tasas de más de 160 monedas que se actualizan solas.

★ Simple y rápido
• Un teclado grande y claro: el resultado cambia mientras escribes
• Intercambia monedas con un toque o añade una tercera para convertir a dos a la vez
• Busca una moneda por país, nombre, símbolo o código, en cualquier idioma
• Funciona sin conexión con las últimas tasas descargadas

★ Convierte desde cualquier app
Selecciona un precio en el navegador, una app de reservas o un chat y elige "Convertir moneda" (o Compartir → "Convertir moneda"). El resultado aparece sobre la app en la que estás. Entiende $, €, ₪, "1.250,00", "1.5k" y más.

★ Historial
Cada importe y cada cálculo se guardan. Toca uno para recuperarlo con sus monedas.

★ Premium
• Widget en la pantalla de inicio: escribe un importe y míralo convertido sin abrir la app
• Calculadora con porcentajes: 120 + 15%, 3 × 800 y más, convertidos mientras escribes
• 6 temas de color, cada uno en claro y oscuro

Los usuarios nuevos tienen Premium gratis durante 3 días. Después, mira 2 vídeos cortos para abrirlo durante 24 horas, o consigue Premium para siempre con un solo pago, que además quita los anuncios. La conversión, la búsqueda, la moneda adicional, el historial y la conversión desde otras apps siempre son gratis.

★ 13 idiomas
Español, inglés, hebreo, árabe, ruso, francés, alemán, portugués, italiano, turco, chino, japonés e hindi.

Las tasas son solo informativas y se actualizan aproximadamente una vez al día. Tu banco o casa de cambio puede aplicar otra tasa.
""" + CREDIT),
    "ar": dict(
        title="محوّل العملات + أداة",
        short="حوّل أكثر من 160 عملة دون إنترنت، من أي تطبيق، مع أداة للشاشة الرئيسية.",
        full="""حوّل أي عملة بلمسة، حتى دون إنترنت. اكتب مبلغًا وشاهده فورًا بعملتك، مع أسعار أكثر من 160 عملة تتحدّث تلقائيًا.

★ بسيط وسريع
• لوحة أرقام كبيرة وواضحة: تتغيّر النتيجة أثناء الكتابة
• بدّل العملات بلمسة، أو أضف عملة ثالثة للتحويل إلى عملتين معًا
• ابحث عن العملة باسم الدولة أو الاسم أو الرمز أو الكود، بأي لغة
• يعمل دون إنترنت بآخر الأسعار التي تم تنزيلها

★ التحويل من أي تطبيق
حدّد سعرًا في المتصفح أو تطبيق حجز أو محادثة واختر "تحويل العملة" (أو مشاركة ← "تحويل العملة"). تظهر النتيجة فوق التطبيق الذي تستخدمه. يفهم $ و€ و₪ و"1.250,00" و"1.5k" وغيرها.

★ السجل
يُحفظ كل مبلغ وكل عملية حسابية. المس أيًّا منها لاستعادتها مع عملاتها.

★ بريميوم
• أداة على الشاشة الرئيسية: اكتب مبلغًا وشاهده محوّلًا دون فتح التطبيق
• آلة حاسبة مع النسب المئوية: 120 + 15% و3 × 800 وغيرها، مع التحويل أثناء الكتابة
• 6 سمات ألوان، كلٌّ منها بوضع فاتح وداكن

يحصل المستخدمون الجدد على بريميوم مجانًا لمدة 3 أيام. بعد ذلك، شاهد مقطعي فيديو قصيرين لفتحه لمدة 24 ساعة، أو احصل على بريميوم مدى الحياة بدفعة واحدة تزيل الإعلانات أيضًا. التحويل والبحث والعملة الإضافية والسجل والتحويل من التطبيقات الأخرى تبقى مجانية دائمًا.

★ 13 لغة
العربية والإنجليزية والعبرية والإسبانية والروسية والفرنسية والألمانية والبرتغالية والإيطالية والتركية والصينية واليابانية والهندية.

الأسعار للعلم فقط وتُحدَّث مرة في اليوم تقريبًا. قد يستخدم المصرف أو مكتب الصرافة سعرًا مختلفًا.
""" + CREDIT),
    "ru-RU": dict(
        title="Конвертер валют + виджет",
        short="Конвертируйте 160+ валют офлайн, из любого приложения, с виджетом.",
        full="""Конвертируйте любую валюту одним касанием, даже без интернета. Введите сумму и сразу увидите её в своей валюте: курсы более 160 валют обновляются сами.

★ Просто и быстро
• Большая понятная клавиатура: результат меняется прямо во время ввода
• Меняйте валюты местами одним касанием или добавьте третью, чтобы видеть сразу две
• Ищите валюту по стране, названию, символу или коду на любом языке
• Работает без интернета с последними загруженными курсами

★ Конвертация из любого приложения
Выделите цену в браузере, приложении бронирования или чате и выберите «Конвертировать валюту» (или «Поделиться» → «Конвертировать валюту»). Результат появится поверх текущего приложения. Понимает $, €, ₪, «1.250,00», «1.5k» и многое другое.

★ История
Каждая сумма и каждый расчёт сохраняются. Коснитесь записи, чтобы вернуть её вместе с валютами.

★ Премиум
• Виджет на главном экране: введите сумму и увидите результат, не открывая приложение
• Калькулятор с процентами: 120 + 15%, 3 × 800 и не только, с конвертацией при вводе
• 6 цветовых тем, каждая в светлом и тёмном варианте

Новые пользователи получают Премиум бесплатно на 3 дня. Потом посмотрите 2 коротких видео, чтобы открыть его на 24 часа, или купите Премиум навсегда одним платежом — он также убирает рекламу. Конвертация, поиск, дополнительная валюта, история и конвертация из других приложений всегда бесплатны.

★ 13 языков
Русский, английский, иврит, арабский, испанский, французский, немецкий, португальский, итальянский, турецкий, китайский, японский и хинди.

Курсы приводятся для справки и обновляются примерно раз в день. В банке или обменном пункте курс может отличаться.
""" + CREDIT),
    "fr-FR": dict(
        title="Convertisseur de devises",
        short="Convertissez 160+ devises hors ligne, depuis toute app, avec un widget.",
        full="""Convertissez n'importe quelle devise d'un geste, même hors ligne. Tapez un montant et voyez-le aussitôt dans votre devise, avec les taux de plus de 160 devises mis à jour automatiquement.

★ Simple et rapide
• Un grand pavé numérique clair : le résultat change pendant la saisie
• Inversez les devises d'un geste ou ajoutez-en une troisième pour convertir vers deux à la fois
• Trouvez une devise par pays, nom, symbole ou code, dans n'importe quelle langue
• Fonctionne hors ligne avec les derniers taux téléchargés

★ Convertissez depuis n'importe quelle app
Sélectionnez un prix dans le navigateur, une app de réservation ou une discussion et choisissez « Convertir une devise » (ou Partager → « Convertir une devise »). Le résultat s'affiche par-dessus l'app ouverte. Comprend $, €, ₪, « 1.250,00 », « 1.5k » et plus encore.

★ Historique
Chaque montant et chaque calcul sont conservés. Touchez-en un pour le retrouver avec ses devises.

★ Premium
• Widget sur l'écran d'accueil : tapez un montant et voyez la conversion sans ouvrir l'app
• Calculatrice avec pourcentages : 120 + 15 %, 3 × 800 et plus, convertis pendant la saisie
• 6 thèmes de couleur, chacun en clair et en sombre

Les nouveaux utilisateurs profitent de Premium gratuitement pendant 3 jours. Ensuite, regardez 2 courtes vidéos pour l'ouvrir 24 heures, ou passez à Premium à vie en un seul paiement, qui supprime aussi les publicités. La conversion, la recherche, la devise supplémentaire, l'historique et la conversion depuis d'autres apps restent toujours gratuits.

★ 13 langues
Français, anglais, hébreu, arabe, espagnol, russe, allemand, portugais, italien, turc, chinois, japonais et hindi.

Les taux sont donnés à titre indicatif et mis à jour environ une fois par jour. Votre banque ou bureau de change peut appliquer un autre taux.
""" + CREDIT),
    "de-DE": dict(
        title="Währungsrechner + Widget",
        short="Über 160 Währungen umrechnen – offline, aus jeder App und mit Widget.",
        full="""Rechne jede Währung mit einem Tippen um, auch offline. Gib einen Betrag ein und sieh ihn sofort in deiner Währung – mit Kursen für über 160 Währungen, die sich selbst aktualisieren.

★ Einfach und schnell
• Große, klare Tastatur: Das Ergebnis ändert sich schon beim Tippen
• Währungen mit einem Tippen tauschen oder eine dritte hinzufügen und in zwei auf einmal umrechnen
• Währungen nach Land, Name, Symbol oder Code suchen, in jeder Sprache
• Funktioniert offline mit den zuletzt geladenen Kursen

★ Aus jeder App umrechnen
Markiere einen Preis im Browser, in einer Buchungs-App oder im Chat und wähle „Währung umrechnen“ (oder Teilen → „Währung umrechnen“). Das Ergebnis erscheint über der App, in der du gerade bist. Versteht $, €, ₪, „1.250,00“, „1.5k“ und mehr.

★ Verlauf
Jeder Betrag und jede Rechnung wird gespeichert. Tippe darauf, um sie mit ihren Währungen zurückzuholen.

★ Premium
• Widget auf dem Startbildschirm: Betrag eingeben und das Ergebnis sehen, ohne die App zu öffnen
• Rechner mit Prozenten: 120 + 15 %, 3 × 800 und mehr, umgerechnet beim Tippen
• 6 Farbschemas, jeweils hell und dunkel

Neue Nutzer bekommen Premium 3 Tage gratis. Danach schalten 2 kurze Videos es für 24 Stunden frei – oder du holst dir Premium für immer mit einer Zahlung, die auch die Werbung entfernt. Umrechnen, Suche, zusätzliche Währung, Verlauf und das Umrechnen aus anderen Apps bleiben immer kostenlos.

★ 13 Sprachen
Deutsch, Englisch, Hebräisch, Arabisch, Spanisch, Russisch, Französisch, Portugiesisch, Italienisch, Türkisch, Chinesisch, Japanisch und Hindi.

Die Kurse dienen nur zur Information und werden etwa einmal täglich aktualisiert. Deine Bank oder Wechselstube kann einen anderen Kurs verwenden.
""" + CREDIT),
    "pt-BR": dict(
        title="Conversor de Moedas + Widget",
        short="Converta mais de 160 moedas offline, de qualquer app e com widget.",
        full="""Converta qualquer moeda com um toque, mesmo offline. Digite um valor e veja na hora na sua moeda, com cotações de mais de 160 moedas que se atualizam sozinhas.

★ Simples e rápido
• Teclado grande e claro: o resultado muda enquanto você digita
• Inverta as moedas com um toque ou adicione uma terceira para converter para duas de uma vez
• Encontre uma moeda pelo país, nome, símbolo ou código, em qualquer idioma
• Funciona offline com as últimas cotações baixadas

★ Converta de qualquer app
Selecione um preço no navegador, num app de reservas ou numa conversa e escolha "Converter moeda" (ou Compartilhar → "Converter moeda"). O resultado aparece sobre o app em que você está. Entende $, €, ₪, "1.250,00", "1.5k" e mais.

★ Histórico
Cada valor e cada cálculo ficam guardados. Toque em um para trazê-lo de volta com as moedas.

★ Premium
• Widget na tela inicial: digite um valor e veja a conversão sem abrir o app
• Calculadora com porcentagens: 120 + 15%, 3 × 800 e mais, convertidos enquanto você digita
• 6 temas de cores, cada um no modo claro e escuro

Novos usuários ganham Premium grátis por 3 dias. Depois, assista a 2 vídeos curtos para liberá-lo por 24 horas, ou tenha Premium para sempre com um único pagamento, que também remove os anúncios. A conversão, a busca, a moeda adicional, o histórico e a conversão a partir de outros apps são sempre grátis.

★ 13 idiomas
Português, inglês, hebraico, árabe, espanhol, russo, francês, alemão, italiano, turco, chinês, japonês e hindi.

As cotações são apenas informativas e atualizadas cerca de uma vez por dia. Seu banco ou casa de câmbio pode usar outra cotação.
""" + CREDIT),
    "it-IT": dict(
        title="Convertitore valute + widget",
        short="Converti oltre 160 valute offline, da qualsiasi app e con un widget.",
        full="""Converti qualsiasi valuta con un tocco, anche offline. Digita un importo e vedilo subito nella tua valuta, con i tassi di oltre 160 valute che si aggiornano da soli.

★ Semplice e veloce
• Un tastierino grande e chiaro: il risultato cambia mentre digiti
• Inverti le valute con un tocco o aggiungine una terza per convertire in due contemporaneamente
• Cerca una valuta per paese, nome, simbolo o codice, in qualsiasi lingua
• Funziona offline con gli ultimi tassi scaricati

★ Converti da qualsiasi app
Seleziona un prezzo nel browser, in un'app di prenotazioni o in una chat e scegli "Converti valuta" (oppure Condividi → "Converti valuta"). Il risultato compare sopra l'app che stai usando. Riconosce $, €, ₪, "1.250,00", "1.5k" e altro.

★ Cronologia
Ogni importo e ogni calcolo vengono salvati. Toccane uno per ritrovarlo con le sue valute.

★ Premium
• Widget nella schermata Home: digita un importo e vedi la conversione senza aprire l'app
• Calcolatrice con percentuali: 120 + 15%, 3 × 800 e altro, convertiti mentre digiti
• 6 temi colore, ognuno chiaro e scuro

I nuovi utenti hanno Premium gratis per 3 giorni. Poi guarda 2 brevi video per sbloccarlo per 24 ore, oppure prendi Premium per sempre con un solo pagamento, che toglie anche la pubblicità. Conversione, ricerca, valuta aggiuntiva, cronologia e conversione da altre app restano sempre gratuite.

★ 13 lingue
Italiano, inglese, ebraico, arabo, spagnolo, russo, francese, tedesco, portoghese, turco, cinese, giapponese e hindi.

I tassi sono solo indicativi e si aggiornano circa una volta al giorno. La tua banca o il cambiavalute possono applicare un tasso diverso.
""" + CREDIT),
    "tr-TR": dict(
        title="Döviz Çevirici + Widget",
        short="160+ para birimini çevrimdışı, her uygulamadan ve widget ile çevirin.",
        full="""Her para birimini tek dokunuşla, internet olmadan bile çevirin. Bir tutar yazın ve hemen kendi para biriminizde görün; 160'tan fazla para biriminin kurları kendiliğinden güncellenir.

★ Basit ve hızlı
• Büyük ve net tuş takımı: sonuç siz yazarken değişir
• Para birimlerini tek dokunuşla değiştirin ya da aynı anda ikisine çevirmek için üçüncü bir para birimi ekleyin
• Para birimini ülke, ad, sembol veya koda göre, her dilde arayın
• İndirilen son kurlarla çevrimdışı çalışır

★ Her uygulamadan çevirin
Tarayıcıda, bir rezervasyon uygulamasında ya da sohbette bir fiyatı seçin ve "Para birimi çevir"i seçin (veya Paylaş → "Para birimi çevir"). Sonuç, bulunduğunuz uygulamanın üzerinde açılır. $, €, ₪, "1.250,00", "1.5k" ve daha fazlasını anlar.

★ Geçmiş
Her tutar ve her hesaplama saklanır. Para birimleriyle birlikte geri getirmek için birine dokunun.

★ Premium
• Ana ekran widget'ı: bir tutar yazın, uygulamayı açmadan çevrilmiş hâlini görün
• Yüzdeli hesap makinesi: 120 + %15, 3 × 800 ve daha fazlası, yazarken çevrilir
• Her biri açık ve koyu olan 6 renk teması

Yeni kullanıcılar Premium'u 3 gün ücretsiz kullanır. Sonrasında 2 kısa video izleyerek 24 saat açabilir ya da reklamları da kaldıran tek bir ödemeyle ömür boyu Premium alabilirsiniz. Çevirme, arama, ek para birimi, geçmiş ve diğer uygulamalardan çevirme her zaman ücretsizdir.

★ 13 dil
Türkçe, İngilizce, İbranice, Arapça, İspanyolca, Rusça, Fransızca, Almanca, Portekizce, İtalyanca, Çince, Japonca ve Hintçe.

Kurlar yalnızca bilgi amaçlıdır ve günde yaklaşık bir kez güncellenir. Bankanız veya döviz bürosu farklı bir kur kullanabilir.
""" + CREDIT),
    "zh-CN": dict(
        title="汇率换算 + 小组件",
        short="离线换算 160 多种货币，可在任何应用中使用，还有主屏幕小组件。",
        full="""轻点一下即可换算任何货币，离线也能用。输入金额，立即看到换算成你的货币的结果，160 多种货币的汇率会自动更新。

★ 简单快速
• 大而清晰的数字键盘：输入时结果实时变化
• 轻点即可互换货币，或添加第三种货币，同时换算成两种
• 可按国家、名称、符号或代码搜索货币，支持任何语言
• 离线时使用最近下载的汇率

★ 在任何应用中换算
在浏览器、订票应用或聊天中选中一个价格，选择"换算货币"（或"分享"→"换算货币"）。结果会直接显示在当前应用上方。支持 $、€、₪、"1.250,00"、"1.5k" 等写法。

★ 历史记录
每一次金额和计算都会保存。轻点一条记录即可连同货币一起恢复。

★ 高级版
• 主屏幕小组件：输入金额，无需打开应用即可看到换算结果
• 带百分比的计算器：120 + 15%、3 × 800 等，边输入边换算
• 6 种颜色主题，每种都有浅色和深色

新用户可免费使用高级版 3 天。之后，观看 2 个短视频即可解锁 24 小时，或一次付款购买永久高级版，同时去除广告。换算、搜索、附加货币、历史记录以及从其他应用换算始终免费。

★ 13 种语言
中文、英语、希伯来语、阿拉伯语、西班牙语、俄语、法语、德语、葡萄牙语、意大利语、土耳其语、日语和印地语。

汇率仅供参考，大约每天更新一次。银行或兑换点的汇率可能不同。
""" + CREDIT),
    "ja-JP": dict(
        title="通貨換算 + ウィジェット",
        short="160以上の通貨をオフラインで換算。どのアプリからでも、ウィジェットでも。",
        full="""タップひとつで、どの通貨もすぐに換算。オフラインでも使えます。金額を入力すると自分の通貨での金額がすぐに表示され、160以上の通貨のレートは自動で更新されます。

★ シンプルで速い
• 大きく見やすいキーパッド：入力中に結果が変わります
• タップで通貨を入れ替え。3つ目の通貨を追加すれば2通貨に同時換算
• 国名、通貨名、記号、コードで、どの言語でも検索可能
• 最後にダウンロードしたレートでオフラインでも動作

★ どのアプリからでも換算
ブラウザや予約アプリ、チャットで価格を選択し、「通貨を換算」を選ぶだけ（または 共有 →「通貨を換算」）。今のアプリの上に結果が表示されます。$、€、₪、「1.250,00」、「1.5k」などにも対応。

★ 履歴
金額も計算もすべて保存。タップすれば通貨ごと呼び出せます。

★ プレミアム
• ホーム画面のウィジェット：アプリを開かずに金額を入力して換算
• パーセント計算もできる電卓：120 + 15%、3 × 800 など、入力しながら換算
• 6つのカラーテーマ（それぞれライトとダーク）

新しいユーザーはプレミアムを3日間無料で使えます。その後は短い動画を2本見ると24時間利用でき、1回の支払いで永久プレミアムにすると広告も消えます。換算、検索、追加の通貨、履歴、他のアプリからの換算はずっと無料です。

★ 13言語
日本語、英語、ヘブライ語、アラビア語、スペイン語、ロシア語、フランス語、ドイツ語、ポルトガル語、イタリア語、トルコ語、中国語、ヒンディー語。

レートは参考値で、1日1回程度更新されます。銀行や両替所では異なるレートが使われる場合があります。
""" + CREDIT),
    "hi-IN": dict(
        title="मुद्रा परिवर्तक + विजेट",
        short="160+ मुद्राएँ ऑफ़लाइन बदलें, किसी भी ऐप से, होम स्क्रीन विजेट के साथ।",
        full="""एक टैप में कोई भी मुद्रा बदलें, बिना इंटरनेट के भी। रकम लिखें और तुरंत अपनी मुद्रा में देखें — 160 से ज़्यादा मुद्राओं की दरें अपने-आप अपडेट होती हैं।

★ आसान और तेज़
• बड़ा, साफ़ कीपैड: लिखते-लिखते नतीजा बदलता है
• एक टैप में मुद्राएँ बदलें, या तीसरी मुद्रा जोड़कर एक साथ दो में बदलें
• देश, नाम, चिह्न या कोड से, किसी भी भाषा में मुद्रा खोजें
• आख़िरी डाउनलोड की गई दरों के साथ ऑफ़लाइन चलता है

★ किसी भी ऐप से बदलें
ब्राउज़र, बुकिंग ऐप या चैट में कोई कीमत चुनें और "मुद्रा बदलें" चुनें (या शेयर करें → "मुद्रा बदलें")। नतीजा उसी ऐप के ऊपर दिखता है जिसमें आप हैं। $, €, ₪, "1.250,00", "1.5k" जैसे लिखावट भी समझता है।

★ इतिहास
हर रकम और हर हिसाब सहेजा जाता है। किसी पर टैप करें और उसे मुद्राओं के साथ वापस लाएँ।

★ प्रीमियम
• होम स्क्रीन विजेट: ऐप खोले बिना रकम लिखें और बदली हुई रकम देखें
• प्रतिशत वाला कैलकुलेटर: 120 + 15%, 3 × 800 और भी, लिखते-लिखते बदला हुआ
• 6 रंग थीम, हर एक लाइट और डार्क में

नए उपयोगकर्ताओं को प्रीमियम 3 दिन मुफ़्त मिलता है। उसके बाद 2 छोटे वीडियो देखकर इसे 24 घंटे के लिए खोलें, या एक बार भुगतान करके हमेशा के लिए प्रीमियम लें, जिससे विज्ञापन भी हट जाते हैं। मुद्रा बदलना, खोज, अतिरिक्त मुद्रा, इतिहास और दूसरे ऐप से बदलना हमेशा मुफ़्त रहते हैं।

★ 13 भाषाएँ
हिन्दी, अंग्रेज़ी, हिब्रू, अरबी, स्पेनिश, रूसी, फ़्रेंच, जर्मन, पुर्तगाली, इतालवी, तुर्की, चीनी और जापानी।

दरें सिर्फ़ जानकारी के लिए हैं और लगभग दिन में एक बार अपडेट होती हैं। आपका बैंक या मनी एक्सचेंज अलग दर ले सकता है।
""" + CREDIT),
}

# Same text for the sister locales Google Play lists separately.
ALIASES = {"es-419": "es-ES", "fr-CA": "fr-FR", "pt-PT": "pt-BR", "en-GB": "en-US"}

LIMITS = {"title": 30, "short": 80, "full": 4000}
FILES = {"title": "title.txt", "short": "short_description.txt", "full": "full_description.txt"}


def main():
    out = sys.argv[1]
    problems = [f"{loc}.{k}: {len(v)} > {LIMITS[k]}" for loc, d in LISTINGS.items() for k, v in d.items() if len(v) > LIMITS[k]]
    if problems:
        print("\n".join(problems))
        sys.exit(1)
    for loc, d in list(LISTINGS.items()) + [(a, LISTINGS[src]) for a, src in ALIASES.items()]:
        os.makedirs(os.path.join(out, loc), exist_ok=True)
        for key, name in FILES.items():
            with open(os.path.join(out, loc, name), "w", encoding="utf-8") as f:
                f.write(d[key].strip() + "\n")
    icon = Image.open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../store/icon-512.png")).convert("RGBA")
    for loc in LISTINGS:
        os.makedirs(os.path.join(out, loc, "images"), exist_ok=True)
        icon.save(os.path.join(out, loc, "images", "icon.png"))
    print(f"{len(LISTINGS) + len(ALIASES)} listings written to {out}")
    for loc, d in LISTINGS.items():
        print(f"  {loc:6} title {len(d['title']):2}  short {len(d['short']):2}  full {len(d['full']):4}")


if __name__ == "__main__":
    main()
