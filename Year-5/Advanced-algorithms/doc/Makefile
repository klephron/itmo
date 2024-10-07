MKDIR := mkdir -p
LIBREOFFICE := libreoffice
PANDOC := pandoc
PANDOC_PDF_ENGINE := xelatex
PDFUNITE := pdfunite

HEADERS_DIR := headers
MERGED_DIR := merged

SRCS.MD = $(wildcard *.md)
SRCS.ODT = $(wildcard *.odt)

TGTS.MD = $(patsubst %.md, %, $(SRCS.MD))
TGTS.ODT = $(patsubst %.odt, %, $(SRCS.ODT))

TGTS = $(sort $(TGTS.MD) $(TGTS.ODT))

HDRS.ODT = $(wildcard $(HEADERS_DIR)/*.odt)
MERGED_TGTS = $(patsubst $(HEADERS_DIR)/%.odt, $(MERGED_DIR)/%, $(HDRS.ODT))


all: merge

merge: $(patsubst %, %.pdf, $(MERGED_TGTS))

build/md: $(patsubst %, %.pdf, $(TGTS.MD))

build/odt: $(patsubst %, %.pdf, $(TGTS.ODT))

clean:
	rm *.pdf 2> /dev/null || true
	rm -rf $(MERGED_DIR) 2> /dev/null || true
	rm $(HEADERS_DIR)/*.pdf 2> /dev/null || true


define make-odt-rule

$(1).pdf: $(1).odt
	$$(LIBREOFFICE)                                                              \
	--headless                                                                   \
	"-env:UserInstallation=file:///tmp/libreoffice_convert_$${USER}_odt_$(1)"    \
	--convert-to pdf:writer_pdf_Export                                           \
	--outdir $$(HEADERS_DIR)                                                     \
	$$<

endef

define make-md-rule

$(1).pdf: $(1).md
	$$(PANDOC) $$< -o $(1).pdf --pdf-engine=$$(PANDOC_PDF_ENGINE) \
	-V mainfont="Liberation Serif" -V papersize:a4                \
	-V geometry:margin=0.75in -V geometry:bottom=0.75in -V geometry:top=0.5in

endef

define make-merge-rule

$$(HEADERS_DIR)/$(1).pdf: $$(HEADERS_DIR)/$(1).odt
	$$(LIBREOFFICE)                                                                \
	--headless                                                                     \
	"-env:UserInstallation=file:///tmp/libreoffice_convert_$${USER}_merge_$(1)"    \
	--convert-to pdf:writer_pdf_Export                                             \
	--outdir $$(HEADERS_DIR)                                                       \
	$$<


$$(MERGED_DIR)/$(1).pdf: $$(HEADERS_DIR)/$(1).pdf $(1).pdf | $$(DIRS)
	$$(PDFUNITE) $$^ $$@

endef


DIRS = $(sort $(MERGED_DIR))

$(sort $(DIRS)):
	$(MKDIR) $(@:dir/%=%)


$(foreach target, $(TGTS.ODT), $(eval $(call make-odt-rule,$(target))))
$(foreach target, $(TGTS.MD), $(eval $(call make-md-rule,$(target))))
$(foreach target, $(TGTS), $(eval $(call make-merge-rule,$(target))))

