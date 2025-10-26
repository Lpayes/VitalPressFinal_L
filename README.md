# Proyecto Final – VitalPress
*Desarrollado por:* Vital Team  
*Universidad Mariano Gálvez de Guatemala – 2025*

---

## Descripción de la Aplicación

*VitalPress* es una aplicación móvil desarrollada en *Android Studio (Java)* que permite a los usuarios registrar, consultar y gestionar sus lecturas de presión arterial de manera sencilla y accesible.

Está diseñada especialmente para personas adultas y usuarios que deseen mantener un control básico de su salud, integrando además funciones de inteligencia artificial, documentos inteligentes y automatización de citas médicas.

La aplicación combina tecnología local y en la nube mediante:

- Base de datos interna (*Room*) para almacenar lecturas.
- *OpenAI API* para generar análisis, respuestas y recomendaciones personalizadas.
- *Google DocumentAI* para escanear documentos médicos e importar datos.
- *n8n* para automatizar la agenda de citas médicas en Google Calendar y enviar correos de confirmación.

---

## Funciones Principales

- *Tomar presión:* Registro manual o digital de presión sistólica, diastólica y pulso.  
  Analiza automáticamente si los valores están dentro de los rangos saludables considerando edad, peso y altura.

- *Escanear documentos:* Integración con *DocumentAI* para importar y procesar lecturas médicas desde archivos PDF o imágenes.

- *Exportar información:* Genera reportes personalizados con análisis de IA y permite exportarlos en formato PDF o CSV.

- *Enviar correos:* Envío automático del reporte al correo del usuario o a contactos médicos configurados.

- *Llamadas de emergencia:* Acceso directo a números de emergencia configurados por el usuario (familiares o servicios médicos).

- *Agendar citas:* Envía la información al flujo de *n8n*, el cual agenda la cita en Google Calendar, notifica al doctor y confirma la cita al usuario por correo electrónico.

---

## Versión de Android

- *Versión mínima:* Android 7.0 (API 24)  
- *Versión objetivo:* Android 14 (API 34)  
- *Compatibilidad:* Teléfonos y tabletas con almacenamiento local y conexión a internet.

---

## Autores

*Proyecto desarrollado por:*

- Mario David Tereta Sapalun — Desarrollo principal e integración con APIs (OpenAI, DocumentAI, n8n)  
- Lester David Payes Méndez — Diseño UI/UX y estructura de Activities

*Equipo:* Vital Team  
*Institución:* Universidad Mariano Gálvez de Guatemala  
*Carrera:* Ingeniería en Sistemas de Información y Ciencias de la Computación  
*Año:* 2025
