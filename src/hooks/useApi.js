import { useCallback, useEffect, useState } from "react";

export const useApi = (apiFunc, ...params) => {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const run = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await apiFunc(...params);
            setData(result);
        } catch (e) {
            setError(e.message || "Error");
        } finally {
            setLoading(false);
        }
    }, [apiFunc, ...params]);

    useEffect(() => {
        run().then();
    }, [run]);

    return { data, loading, error, refetch: run };
}
