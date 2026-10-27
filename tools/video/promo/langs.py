"""The promo's words in every listing language (build.py <lang>). Hebrew is the original; the others follow the store
screenshots (tools/store/screenshots.mjs COPY: cur, off, sets, the colours headline) and the app's own strings (premium,
the selection-menu item), so the video, the listing and the app say the same thing.

Per language: the page's price and the bubble's result (tools/preview/render/Render.kt storeLangs and storeRates: what
the 4_quick screenshot shows), the store title (tools/store/listing.py) and the video's own lines."""

# The tour page's price and the converted amount the bubble shows (store_<lang>_4_quick.png).
BOARD = {
    'iw': ('€189', '₪763.38'), 'en': ('€189', '$205.64'), 'es': ('$189', '€173.71'), 'ar': ('€189', 'SAR 771.13'),
    'ru': ('€189', '₽19,017.21'), 'fr': ('$189', '€173.71'), 'de': ('$189', '€173.71'), 'pt': ('€189', 'R$1,114.14'),
    'it': ('$189', '€173.71'), 'tr': ('€189', '₺7,047.14'), 'zh': ('€189', '¥1,483.46'), 'ja': ('€189', '¥31,100.38'),
    'hi': ('€189', '₹17,287.81'),
}

# wd: the widgets headline (plain, block); st: "without opening the app"; cs: the colours headline and its block (the
# number goes first); an: the any-app headline; press: the long-press sticker; cur/off: the claim; cta: the end card.
T = {
    'iw': dict(wd=("הווידג'ט", 'על המסך הראשי.'), st='בלי לפתוח את האפליקציה', cs=('הצבעים שלך.', 'ערכות צבעים'),
               an=('מכל אתר', 'ומכל אפליקציה.'), press='לחיצה ארוכה, וזהו.', cur='מטבעות', off='גם בלי אינטרנט.',
               cta='חינם ב-Google Play', stop='.'),
    'en': dict(wd=('The widget', 'on your home screen.'), st='No need to open the app', cs=('Your colors.', 'color sets'),
               an=('From any website', 'and any app.'), press='Long-press. Done.', cur='currencies', off='Works offline.',
               cta='Free on Google Play', stop='.'),
    'es': dict(wd=('El widget', 'en tu pantalla de inicio.'), st='Sin abrir la app', cs=('Tus colores.', 'temas de color'),
               an=('Desde cualquier web', 'y cualquier app.'), press='Mantén pulsado. Listo.', cur='monedas',
               off='Funciona sin conexión.', cta='Gratis en Google Play', stop='.'),
    'ar': dict(wd=('الأداة', 'على الشاشة الرئيسية.'), st='دون فتح التطبيق', cs=('ألوانك.', 'سمات لونية'),
               an=('من أي موقع', 'ومن أي تطبيق.'), press='ضغطة مطوّلة، وهذا كل شيء.', cur='عملة', off='يعمل دون إنترنت.',
               cta='مجانًا على Google Play', stop='.'),
    'ru': dict(wd=('Виджет', 'на главном экране.'), st='Не открывая приложение', cs=('Ваши цвета.', 'цветовых тем'),
               an=('С любого сайта', 'и из любого приложения.'), press='Долгое нажатие — и всё.', cur='валют',
               off='Работает без интернета.', cta='Бесплатно в Google Play', stop='.'),
    'fr': dict(wd=('Le widget', "sur l'écran d'accueil."), st="Sans ouvrir l'app", cs=('Vos couleurs.', 'thèmes de couleur'),
               an=('Sur tous les sites', 'et dans toutes les apps.'), press='Un appui long, et voilà.', cur='devises',
               off='Fonctionne hors ligne.', cta='Gratuit sur Google Play', stop='.'),
    'de': dict(wd=('Das Widget', 'auf dem Startbildschirm.'), st='Ohne die App zu öffnen', cs=('Deine Farben.', 'Farbschemas'),
               an=('Auf jeder Website', 'und in jeder App.'), press='Lange drücken. Fertig.', cur='Währungen',
               off='Funktioniert offline.', cta='Kostenlos bei Google Play', stop='.'),
    'pt': dict(wd=('O widget', 'na tela inicial.'), st='Sem abrir o app', cs=('Suas cores.', 'temas de cores'),
               an=('Em qualquer site', 'e em qualquer app.'), press='Toque e segure. Pronto.', cur='moedas',
               off='Funciona offline.', cta='Grátis no Google Play', stop='.'),
    'it': dict(wd=('Il widget', 'nella schermata Home.'), st="Senza aprire l'app", cs=('I tuoi colori.', 'temi di colore'),
               an=('Da qualsiasi sito', 'e da qualsiasi app.'), press='Tieni premuto. Fatto.', cur='valute',
               off='Funziona offline.', cta='Gratis su Google Play', stop='.'),
    'tr': dict(wd=('Widget', 'ana ekranında.'), st='Uygulamayı açmadan', cs=('Senin renklerin.', 'renk teması'),
               an=('Her siteden', 've her uygulamadan.'), press='Basılı tut, bu kadar.', cur='para birimi',
               off='İnternetsiz çalışır.', cta="Google Play'de ücretsiz", stop='.'),
    'zh': dict(wd=('小组件', '就在主屏幕上。'), st='无需打开应用', cs=('你的颜色。', '套颜色主题'),
               an=('任何网站', '任何应用。'), press='长按一下，就这么简单。', cur='种货币', off='离线也能用。',
               cta='Google Play 免费下载', stop='。'),
    'ja': dict(wd=('ウィジェットを', 'ホーム画面に。'), st='アプリを開かずに', cs=('好きな色で。', '種類のカラーテーマ'),
               an=('どのサイトでも', 'どのアプリでも。'), press='長押しするだけ。', cur='通貨', off='オフラインでも使える。',
               cta='Google Play で無料', stop='。'),
    'hi': dict(wd=('विजेट', 'होम स्क्रीन पर।'), st='ऐप खोले बिना', cs=('आपके रंग।', 'रंग थीम'),
               an=('किसी भी साइट से', 'और किसी भी ऐप से।'), press='देर तक दबाएँ, बस।', cur='मुद्राएँ',
               off='ऑफ़लाइन भी चलता है।', cta='Google Play पर मुफ़्त', stop='।'),
}

# The store title (tools/store/listing.py) for the end card.
TITLE = {
    'iw': "ממיר מטבעות + וידג'ט", 'en': 'Currency Converter + Widget', 'es': 'Conversor de divisas + widget',
    'ar': 'محوّل العملات + أداة', 'ru': 'Конвертер валют + виджет', 'fr': 'Convertisseur de devises',
    'de': 'Währungsrechner + Widget', 'pt': 'Conversor de moedas + widget', 'it': 'Convertitore valute + widget',
    'tr': 'Döviz Çevirici + Widget', 'zh': '汇率换算 + 小组件', 'ja': '通貨換算 + ウィジェット', 'hi': 'मुद्रा परिवर्तक + विजेट',
}

# The phone's text-selection menu (tools/store/scenes.mjs SELECTION) and the app's item (quick_convert_label).
MENU = {
    'iw': ('העתקה', 'שיתוף', 'המרת מטבע'), 'en': ('Copy', 'Share', 'Convert currency'),
    'es': ('Copiar', 'Compartir', 'Convertir moneda'), 'ar': ('نسخ', 'مشاركة', 'تحويل العملة'),
    'ru': ('Копировать', 'Поделиться', 'Конвертировать валюту'), 'fr': ('Copier', 'Partager', 'Convertir une devise'),
    'de': ('Kopieren', 'Teilen', 'Währung umrechnen'), 'pt': ('Copiar', 'Compartilhar', 'Converter moeda'),
    'it': ('Copia', 'Condividi', 'Converti valuta'), 'tr': ('Kopyala', 'Paylaş', 'Döviz çevir'),
    'zh': ('复制', '分享', '货币换算'), 'ja': ('コピー', '共有', '通貨を換算'), 'hi': ('कॉपी करें', 'शेयर करें', 'मुद्रा कन्वर्ट करें'),
}

PREMIUM = {'iw': 'פרימיום', 'ar': 'بريميوم', 'ru': 'Премиум', 'zh': '高级版', 'ja': 'プレミアム', 'hi': 'प्रीमियम'}

# Google Play locale for the file name (store/video/promo-<code>.mp4; Hebrew stays promo-he).
FILE = {'iw': 'he'}

RTL = {'iw', 'ar'}
# Fonts for the words: Secular One draws Hebrew (as in the store screenshots); Latin and Cyrillic use bold Rubik, the
# other scripts bold Noto from @fontsource (tools/store/.fonts, fetched by tools/store/screenshots.mjs).
NOTO = {'ar': ('noto-sans-arabic', 'Noto Sans Arabic'), 'hi': ('noto-sans-devanagari', 'Noto Sans Devanagari'),
        'zh': ('noto-sans-sc', 'Noto Sans SC'), 'ja': ('noto-sans-jp', 'Noto Sans JP')}
