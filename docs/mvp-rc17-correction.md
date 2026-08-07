# MVP RC17 correction — import author selector layout

## Problem

The generic `.import-form input` rule gave every input `width: 100%`, including radio buttons in the author selector. This made each radio consume the form row and pushed its label text to the far right of the fieldset.

## Correction

Radio inputs inside `.radio-option` now explicitly reset width, height, min-height, padding and flex sizing. The label text therefore stays directly beside the radio control.

The React behavior is unchanged: `Namn` and `E-post` are rendered only when `Någon annan` is selected.

## Scope

Frontend CSS only. No API, persistence, Git, work-branch, authentication or production configuration changes.
