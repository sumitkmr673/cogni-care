import { useCallback, useEffect, useState } from "react";
import { clearToken, getCurrentUser, hasToken, login } from "../api";

export default function useAuth() {
  const [status, setStatus] = useState("checking");
  const [caregiver, setCaregiver] = useState(null);

  useEffect(() => {
    let active = true;
    if (!hasToken()) {
      setStatus("unauthenticated");
      return () => { active = false; };
    }

    getCurrentUser()
      .then((user) => {
        if (active) {
          setCaregiver(user);
          setStatus("authenticated");
        }
      })
      .catch(() => {
        clearToken();
        if (active) {
          setCaregiver(null);
          setStatus("unauthenticated");
        }
      });

    return () => { active = false; };
  }, []);

  const signIn = useCallback(async (email, password) => {
    const user = await login(email, password);
    setCaregiver(user);
    setStatus("authenticated");
    return user;
  }, []);

  const signOut = useCallback(() => {
    clearToken();
    setCaregiver(null);
    setStatus("unauthenticated");
  }, []);

  return { status, caregiver, signIn, signOut };
}
