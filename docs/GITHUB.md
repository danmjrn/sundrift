# GitHub verification

The original repository was renamed in place to **[danmjrn/noirstay](https://github.com/danmjrn/noirstay)**
using the existing authenticated owner browser session. No replacement repository was created.

- The browser account menu identified `danmjrn`.
- The target was not found in that owner session, and GitHub's rename form explicitly
  reported **noirstay is available** before the rename was applied.
- GitHub returned `https://github.com/danmjrn/noirstay`, owner/name `danmjrn/noirstay`.
- SSH `ls-remote` verified original `main` at `c3ad278b8433ea575b4e79c951d5682176798393`
  and the already-pushed feature milestone at `865880e0fd5b98d8050e8ca50d70d32cfbdbe026`.
- Existing branches/history/ownership were preserved by the native rename operation.
- The exact requested GitHub description and `java`, `spring-boot`, `nextjs`, `typescript`,
  `postgresql`, `booking-system`, `full-stack`, `portfolio` topics were saved and re-read.
- Website/homepage was left empty: no working public deployment target was supplied.

The local remote is `git@github.com:danmjrn/noirstay.git`. SSH authentication confirmed
`danmjrn`, and pushes use SSH as requested. An old GitHub CLI HTTP credential returned 401;
the working browser session handled repository administration instead. That credential was
not printed, changed or rotated. No GitHub plugin connection is claimed.

Work is on **[feat/noirstay-portfolio](https://github.com/danmjrn/noirstay/tree/feat/noirstay-portfolio)**.
The default branch is preserved for review; no force-push, branch-protection change or
merge is performed. The final verification document records the release commit and PR.

For subsequent authorized updates:

```sh
git remote set-url origin git@github.com:danmjrn/noirstay.git
git push origin feat/noirstay-portfolio
```

The original repository URL redirects to the renamed repository. Historical references
in NOTICE and the case study intentionally retain the original project name.
