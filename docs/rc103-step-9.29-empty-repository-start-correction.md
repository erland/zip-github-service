# rc.103 — step 9.29 empty repository start correction

`Starta arbete` now prepares a completely empty GitHub repository before internal project persistence. The flow is: verify user/repository access, prove there are no branches, create the empty root commit/default branch, verify that branch through GitHub, then persist the project and provision the ordinary Work branch.

No ZIP contents are introduced by bootstrap. Existing initialized repositories are never auto-repaired. Bootstrap failures are returned as explicit API problem codes so the UI no longer collapses this path to `The request could not be completed.`
