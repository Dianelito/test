import React from 'react';
import { Button } from "@/components/ui/button"
import { useLanguage } from "../contexts/LanguageContext"

export function LanguageToggle() {
  const { language, setLanguage } = useLanguage()

  return (
    <Button variant="ghost" onClick={() => setLanguage(language === 'es' ? 'en' : 'es')}>
      {language === 'es' ? 'EN' : 'ES'}
    </Button>
  )
}
