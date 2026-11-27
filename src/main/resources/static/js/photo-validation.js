(() => {
    const MAX_PHOTO_SIZE_BYTES = 2 * 1024 * 1024;
    const MAX_PHOTO_SIZE_MESSAGE = "La fotografía no debe superar los 2 MB.";

    document.querySelectorAll("[data-photo-input]").forEach((input) => {
        const form = input.form;
        const feedback = form?.querySelector("[data-photo-feedback]");
        if (!form || !feedback) {
            return;
        }

        const validateSize = () => {
            const file = input.files?.[0];
            const oversized = Boolean(file && file.size > MAX_PHOTO_SIZE_BYTES);

            input.setCustomValidity(oversized ? MAX_PHOTO_SIZE_MESSAGE : "");
            input.classList.toggle("is-invalid", oversized);
            if (oversized) {
                input.setAttribute("aria-invalid", "true");
                feedback.textContent = MAX_PHOTO_SIZE_MESSAGE;
                feedback.classList.add("is-invalid");
            } else {
                input.removeAttribute("aria-invalid");
                feedback.textContent = "";
                feedback.classList.remove("is-invalid");
            }

            return !oversized;
        };

        input.addEventListener("change", validateSize);
        input.addEventListener("invalid", validateSize);
        form.addEventListener("submit", (event) => {
            if (!validateSize()) {
                event.preventDefault();
                input.focus();
            }
        });
    });
})();
