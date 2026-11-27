const PHONE_RULES = {
    CELULAR: {
        maxDigits: 9,
        pattern: "[0-9]{9}",
        help: "Ingresa 9 dígitos. Ejemplo: 987654321",
        label: "Número de celular",
        invalidMessage: "El número de celular debe tener 9 dígitos.",
        validMessage: "Número de celular válido."
    },
    FIJO: {
        maxDigits: 7,
        pattern: "[0-9]{7}",
        help: "Ingresa 7 dígitos. Ejemplo: 4567890",
        label: "Número de teléfono fijo",
        invalidMessage: "El número de teléfono fijo debe tener 7 dígitos.",
        validMessage: "Número de teléfono fijo válido."
    }
};

function phoneRowsIn(form) {
    return form.matches("[data-phone-row]")
        ? [form]
        : Array.from(form.querySelectorAll("[data-phone-row]"));
}

function setFeedback(input, feedback, valid, message, visible) {
    input.setCustomValidity(valid ? "" : message);
    if (!visible) {
        input.classList.remove("is-valid", "is-invalid");
        input.removeAttribute("aria-invalid");
        feedback.textContent = "";
        feedback.classList.remove("is-valid", "is-invalid");
        return;
    }

    input.classList.toggle("is-valid", valid);
    input.classList.toggle("is-invalid", !valid);
    input.setAttribute("aria-invalid", valid ? "false" : "true");
    feedback.classList.toggle("is-valid", valid);
    feedback.classList.toggle("is-invalid", !valid);
    feedback.textContent = message;
}

function updatePhoneRow(row, clearValues = false) {
    const typeField = row.querySelector("[data-phone-type]");
    const areaField = row.querySelector("[data-phone-area]");
    const areaContainer = row.querySelector("[data-phone-area-field]");
    const areaFeedback = row.querySelector("[data-phone-area-feedback]");
    const numberField = row.querySelector("[data-phone-number]");
    const numberHelp = row.querySelector("[data-phone-number-help]");
    const numberLabel = row.querySelector("[data-phone-number-label]");
    const numberFeedback = row.querySelector("[data-phone-number-feedback]");
    if (!typeField || !areaField || !areaContainer || !areaFeedback
            || !numberField || !numberHelp || !numberLabel || !numberFeedback) {
        return;
    }

    const rules = PHONE_RULES[typeField.value] || PHONE_RULES.CELULAR;
    if (clearValues) {
        areaField.value = "";
        numberField.value = "";
        row.dataset.phoneTouched = "false";
    }

    const isCell = typeField.value === "CELULAR";
    areaContainer.hidden = isCell;
    areaField.disabled = isCell;
    areaField.required = !isCell;
    areaField.inputMode = "numeric";
    areaField.maxLength = 3;
    areaField.pattern = "[0-9]{2,3}";
    if (isCell) {
        areaField.value = "";
        areaField.setCustomValidity("");
        areaFeedback.textContent = "";
        areaField.classList.remove("is-valid", "is-invalid");
        areaField.removeAttribute("aria-invalid");
        areaFeedback.classList.remove("is-valid", "is-invalid");
    } else {
        const areaValid = /^[0-9]{2,3}$/.test(areaField.value);
        const showAreaFeedback = row.dataset.phoneTouched === "true" || areaField.value.length > 0;
        setFeedback(areaField, areaFeedback, areaValid,
            "El código de área debe contener 2 o 3 dígitos.", showAreaFeedback);
    }

    numberField.inputMode = "numeric";
    numberField.maxLength = rules.maxDigits;
    numberField.pattern = rules.pattern;
    numberHelp.textContent = rules.help;
    numberLabel.textContent = rules.label;
    validatePhoneNumber(row);
}

function validatePhoneNumber(row, showFeedback = row.dataset.phoneTouched === "true") {
    const typeField = row.querySelector("[data-phone-type]");
    const numberField = row.querySelector("[data-phone-number]");
    const feedback = row.querySelector("[data-phone-number-feedback]");
    if (!typeField || !numberField || !feedback) {
        return true;
    }

    const rules = PHONE_RULES[typeField.value] || PHONE_RULES.CELULAR;
    const valid = new RegExp(`^[0-9]{${rules.maxDigits}}$`).test(numberField.value);
    const visible = showFeedback || numberField.value.length > 0;
    setFeedback(numberField, feedback, valid, valid ? rules.validMessage : rules.invalidMessage, visible);
    return valid;
}

function sanitizeDigits(input) {
    const digitsOnly = input.value.replace(/[^0-9]/g, "").slice(0, input.maxLength);
    if (input.value !== digitsOnly) {
        input.value = digitsOnly;
    }
}

function validatePhoneForm(form, showFeedback = false) {
    const rows = phoneRowsIn(form);
    let valid = rows.length > 0;
    rows.forEach((row) => {
        const numberValid = validatePhoneNumber(row, showFeedback);
        const areaField = row.querySelector("[data-phone-area]");
        const areaValid = !areaField || areaField.disabled || /^[0-9]{2,3}$/.test(areaField.value);
        if (areaField && !areaField.disabled) {
            const feedback = row.querySelector("[data-phone-area-feedback]");
            setFeedback(areaField, feedback, areaValid,
                "El código de área debe contener 2 o 3 dígitos.",
                showFeedback || areaField.value.length > 0);
        }
        valid = numberValid && areaValid && valid;
    });
    return valid;
}

document.querySelectorAll("[data-phone-row]").forEach((row) => updatePhoneRow(row));

document.querySelectorAll("form[data-phone-form]").forEach((form) => {
    form.addEventListener("invalid", (event) => {
        if (event.target.matches("[data-phone-number], [data-phone-area]")) {
            const row = event.target.closest("[data-phone-row]") || form;
            row.dataset.phoneTouched = "true";
            validatePhoneForm(form, true);
        }
    }, true);
    form.addEventListener("submit", (event) => {
        if (!validatePhoneForm(form, true)) {
            event.preventDefault();
            form.querySelector("[aria-invalid='true']")?.focus();
        }
    });
});

document.addEventListener("change", (event) => {
    if (event.target.matches("[data-phone-type]")) {
        updatePhoneRow(event.target.closest("[data-phone-row]"), true);
    }
});

document.addEventListener("input", (event) => {
    if (event.target.matches("[data-phone-number], [data-phone-area]")) {
        const row = event.target.closest("[data-phone-row]");
        row.dataset.phoneTouched = "true";
        sanitizeDigits(event.target);
        updatePhoneRow(row);
        validatePhoneNumber(row, true);
    }
});

document.querySelectorAll("[data-add-row]").forEach((button) => {
    const collection = document.getElementById(button.dataset.addRow);
    if (!collection) {
        return;
    }

    const group = collection.dataset.collection;
    const rows = () => Array.from(collection.querySelectorAll("[data-row]"));

    const reindex = () => {
        rows().forEach((row, index) => {
            row.querySelectorAll("[name]").forEach((field) => {
                field.name = field.name.replace(new RegExp(`${group}\\[\\d+\\]`), `${group}[${index}]`);
            });
            row.querySelectorAll("[id]").forEach((element) => {
                element.id = element.id.replace(/-\d+$/, `-${index}`);
            });
            row.querySelectorAll("label[for]").forEach((label) => {
                label.htmlFor = label.htmlFor.replace(/-\d+$/, `-${index}`);
            });
            row.querySelectorAll("[aria-describedby]").forEach((element) => {
                const ids = element.getAttribute("aria-describedby").split(/\s+/)
                    .map((id) => id.replace(/-\d+$/, `-${index}`));
                element.setAttribute("aria-describedby", ids.join(" "));
            });
            const legend = row.querySelector("legend");
            if (legend) {
                legend.textContent = group === "phones" ? `Teléfono ${index + 1}` : `Correo ${index + 1}`;
            }
        });
    };

    button.addEventListener("click", () => {
        const lastRow = rows().at(-1);
        if (!lastRow) {
            return;
        }
        const newRow = lastRow.cloneNode(true);
        newRow.querySelectorAll("input").forEach((input) => {
            input.value = "";
            input.setCustomValidity("");
            input.classList.remove("is-valid", "is-invalid");
            input.removeAttribute("aria-invalid");
        });
        newRow.querySelectorAll("[data-phone-number-feedback], [data-phone-area-feedback]")
            .forEach((feedback) => {
                feedback.textContent = "";
                feedback.classList.remove("is-valid", "is-invalid");
            });
        newRow.querySelectorAll("select").forEach((select) => select.selectedIndex = 0);
        newRow.dataset.phoneTouched = "false";
        collection.appendChild(newRow);
        reindex();
        if (group === "phones") {
            updatePhoneRow(newRow);
        }
    });

    collection.addEventListener("click", (event) => {
        const removeButton = event.target.closest("[data-remove-row]");
        if (removeButton && rows().length > 1) {
            removeButton.closest("[data-row]").remove();
            reindex();
        }
    });
});
