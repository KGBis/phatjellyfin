# PhatJellyfin

> "Because your Jellyfin deserves proper metadata too."

A Jellyfin CLI music tag importer for people who need to preserve old tags without accents and modern media servers without going nuts.

### What does this program do?
PhatJellyfin reads custom metadata from your music files and imports it into Jellyfin, while leaving the original tags untouched.

### Why?

Well, I own an old Kenwood car audio head with a Phatnoise cartridge, in fact a SDD, which mimics a CD loader. It's a 
really cool gadget and, in my humble opinion, far better than a Spotify subscription or a pendrive as it packages a lot 
of features.

The downside is that is a not internationalized device so any non-english character like accented vowels, the spanish _ñ_ 
or german umlaut is replaced by am ugly blank space. My approach was to replace any of those with the ascii approach, so
the _ñ_ became _n_ or the _ö_ became _o_, and so on. It's great for the Kenwood but a pity to see in your Jellyfin managed 
library.

That's it. The reason behind this program is: _"Because your Jellyfin deserves proper metadata too."_
