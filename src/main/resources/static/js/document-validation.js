(() => {
    const documentRules = {
        DNI: { length: 8, message: "El DNI debe tener exactamente 8 dígitos.", validMessage: "DNI válido." },
        CARNET_EXTRANJERIA: {
            length: 11,
            message: "El Carnet de Extranjería debe tener exactamente 11 dígitos.",
            validMessage: "Carnet de Extranjería válido."
        },
        RUC: { length: 11, message: "El RUC debe tener exactamente 11 dígitos.", validMessage: "RUC válido." }
    };

    document.querySelectorAll("form[data-document-validation]").forEach((form) => {
        const documentType = form.querySelector("[data-document-type]");
        const documentNumber = form.querySelector("[data-document-number]");
        const feedback = form.querySelector("[data-document-feedback]");
        if (!documentNumber || !feedback) {
            return;
        }

        let rule;
        let touched = documentNumber.value.length > 0;
        let invalidCharacters = false;

        const renderFeedback = (message, state) => {
            feedback.textContent = message;
            feedback.classList.toggle("is-invalid", state === "invalid");
            feedback.classList.toggle("is-valid", state === "valid");
            documentNumber.classList.toggle("is-invalid", state === "invalid");
            documentNumber.classList.toggle("is-valid", state === "valid");
            documentNumber.setAttribute("aria-invalid", state === "invalid" ? "true" : "false");
        };

        const validate = (showEmptyError = false) => {
            const value = documentNumber.value;
            const valid = new RegExp(`^[0-9]{${rule.length}}$`).test(value);
            if (invalidCharacters) {
                documentNumber.setCustomValidity("Ingrese únicamente números.");
                renderFeedback("Error: Ingrese únicamente números.", "invalid");
                return false;
            }
            if (valid) {
                documentNumber.setCustomValidity("");
                renderFeedback(`Válido: ${rule.validMessage}`, "valid");
                return true;
            }

            documentNumber.setCustomValidity(rule.message);
            if (value.length > 0 || touched || showEmptyError) {
                renderFeedback(`Error: ${rule.message}`, "invalid");
            } else {
                renderFeedback("", "neutral");
            }
            return false;
        };

        const updateRule = (clearValue) => {
            const kind = documentType ? documentType.value : form.dataset.documentKind;
            rule = documentRules[kind];
            if (!rule) {
                return;
            }
            documentNumber.maxLength = rule.length;
            documentNumber.setAttribute("maxlength", String(rule.length));
            documentNumber.setAttribute("pattern", `[0-9]{${rule.length}}`);
            if (clearValue) {
                documentNumber.value = "";
                touched = false;
                invalidCharacters = false;
            }
            validate(false);
        };

        documentNumber.addEventListener("beforeinput", (event) => {
            if (event.inputType.startsWith("insert") && event.data && /[^0-9]/.test(event.data)) {
                event.preventDefault();
                invalidCharacters = true;
                validate(true);
            }
        });

        documentNumber.addEventListener("paste", (event) => {
            const pastedText = event.clipboardData.getData("text");
            if (/[^0-9]/.test(pastedText)) {
                event.preventDefault();
                invalidCharacters = true;
                validate(true);
            }
        });

        documentNumber.addEventListener("input", () => {
            const sanitizedValue = documentNumber.value.replace(/[^0-9]/g, "").slice(0, rule.length);
            if (sanitizedValue !== documentNumber.value) {
                documentNumber.value = sanitizedValue;
                invalidCharacters = true;
            } else {
                invalidCharacters = false;
            }
            touched = true;
            validate(true);
        });

        documentNumber.addEventListener("invalid", () => validate(true));
        if (documentType) {
            documentType.addEventListener("change", () => updateRule(true));
        }
        form.addEventListener("submit", (event) => {
            touched = true;
            if (!validate(true)) {
                event.preventDefault();
                documentNumber.focus();
            }
        });

        updateRule(false);
    });
})();
