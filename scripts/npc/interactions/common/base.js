const ScriptInteractionResult = {
    NoHandler: -2,
    HandlerError: -3,
    Dispose: -9
}

/**
 * Class representing a registry for interaction handlers.
 */
class InteractionHandlerRegistry {
    constructor() {
        this.handlers = new Map();
        this.defaultHandlers = new Map();
    }

    /**
     * Registers a handler for a specific status and selection.
     * @param {number} status - The status to register the handler for.
     * @param {number|null} selection - The selection to register the handler for. Use null if the selection is not needed when determining the handler to use.
     * @param {Function} handler - The handler function to register.
     */
    register(status, selection, handler) {
        if (selection === null) {
            if (!this.defaultHandlers.has(status)) {
                this.defaultHandlers.set(status, handler);
            }
        } else {
            if (!this.handlers.has(status)) {
                this.handlers.set(status, new Map());
            }

            this.handlers.get(status).set(selection, handler);
        }
    }

    /**
     * Retrieves the handler for a specific status and selection.
     * @param {number} status - The status to retrieve the handler for.
     * @param {number|null} selection - The selection to retrieve the handler for.
     * @returns {Function} The handler function.
     * @throws {Error} If no handler is found for the given status and selection.
     */
    getHandler(status, selection) {
        const statusHandlers = this.handlers.get(status);
        if (statusHandlers && statusHandlers.has(selection)) {
            return statusHandlers.get(selection);
        }

        const defaultHandler = this.defaultHandlers.get(status);
        if (defaultHandler) {
            return defaultHandler;
        }

        throw new Error(`No handler found for status: ${status}, selection: ${selection}`);
    }

    /**
     * Clears all registered handlers.
     */
    clear() {
        this.handlers.clear();
        this.defaultHandlers.clear();
    }
}