LIBREOFFICE := libreoffice
PANDOC := pandoc
PANDOC_PDF_ENGINE := xelatex

SRCS.MD = $(wildcard *.md)
SRCS.ODT = $(wildcard *.odt)

TGTS.MD = $(patsubst %.md, %, $(SRCS.MD))
TGTS.ODT = $(patsubst %.odt, %, $(SRCS.ODT))

all: build/md

build/md: $(patsubst %, md/%.pdf, $(TGTS.ODT))

build/odt: $(patsubst %, odt/%.pdf, $(TGTS.ODT))

clean:
	rm *.pdf 2> /dev/null || true

define make-odt-rule

odt/$(1).pdf: $(1).odt
	$$(LIBREOFFICE) --convert-to pdf $$<

endef

define make-md-rule

md/$(1).pdf: $(1).md
	$$(PANDOC) $$< -o $(1).pdf --pdf-engine=$$(PANDOC_PDF_ENGINE) -V mainfont="Liberation Serif" -V geometry:margin=0.5in

endef

$(foreach target, $(TGTS.ODT), $(eval $(call make-odt-rule,$(target))))
$(foreach target, $(TGTS.MD), $(eval $(call make-md-rule,$(target))))
