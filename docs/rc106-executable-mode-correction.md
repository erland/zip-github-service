# rc.106 executable-mode packaging correction

rc.105 was repackaged from an extracted working tree whose Unix execute bits had not been preserved.
That caused executable shell scripts and `backend/mvnw` to appear as mode changes in the authoritative ZIP snapshot.

rc.106 restores those modes from rc.103, keeps the rc.104/rc.105 application changes intact, and adds
archive verification requiring every `.sh` file and `backend/mvnw` to remain executable.
