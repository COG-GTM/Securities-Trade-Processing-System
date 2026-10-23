Golden custodian MT548 drops for `StatusArchiveReaderTest`, built with GNU tar 1.34 (not commons-compress), so
they pin what custodians send rather than what our library writes:

    tar --format=<ustar|gnu|pax> --blocking-factor=1 --no-recursion --owner=0 --group=0 --mtime='2027-10-13 18:00Z' -cf <file> ...

- `ustar-two-messages.tar`: directory `2027-10-13/`, then `euroclear-0001.mt548`, `clearstream-0002.mt548` (UTF-8 `Zürich`)
- `gnu-longname.tar`, `pax-longname.tar`: one message under a 151-char path (GNU `././@LongLink` / PAX `path=`)
