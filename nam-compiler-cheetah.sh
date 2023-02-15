#!/bin/bash
#
# Compile a NAM controller _F_A_S_T_ (experimental).
#
# Requires bash perl sed tail dd iconv xargs.
set -e
print_usage() {
cat << EOM
Compile a NAM controller _F_A_S_T_ (experimental). Usage:

  ./nam-compiler-cheetah.sh [--lhd] [--low-ram] [ <input> [<output>] ]

where   <input>       Controller directory (defaults to ./Controller/)
        <output>      directory or .dat file (defaults to ./NetworkAddonMod_Controller.dat)
        --lhd         compile a LHD controller (defaults to RHD)
        --low-ram     compile a smaller controller excluding RHW
        -h, --help    display this help

Not for productive use as the resulting controller file can differ from the output of the ordinary compiler.
EOM
}
########################################

# handling of input arguments
for arg in "$@"; do
    shift
    case "$arg" in
        '-h')     print_usage; exit 0 ;;
        '--help') print_usage; exit 0 ;;
        '--low-ram') lowram="1" ;;
        '--lhd') lhd="1" ;;
        '--rhd') ;;
        *) set -- "$@" "$arg" ;;
    esac
done
INDIR="${1:-./Controller}"
INDIR="${INDIR%/}"  # removes trailing slash
OUT="${2:-.}"
if [[ "$OUT" != *.dat ]]; then
    # assumes that OUT was a directory
    OUT="${OUT%/}/NetworkAddonMod_Controller.dat"
fi

if [ -z ${lhd+x} ]; then driveside='RHD'; else driveside='LHD'; fi
if [ -z ${lhd+x} ]; then drivesideignore='*LHD.*'; else drivesideignore='*RHD.*'; fi

directories=("RUL0" "RUL1" "RUL2" "INI" "LText")
tgis=("0x0A5BCF4B" "0xAA5BCF57" "0x10000000"
      "0x0A5BCF4B" "0xAA5BCF57" "0x10000001"
      "0x0A5BCF4B" "0xAA5BCF57" "0x10000002"
      "0x00000000" "0x8A5971C5" "0x8A5993B9"
      "0x2026960B" "0x123006AA" "0x6A47FFFF")

for d in "${directories[@]}"
do
    # error out before doing anything harmful
    if [[ ! -d "$INDIR/$d" && "$d" != "${directories[-1]}" ]]; then
        echo "Error: Directory ‘$INDIR/$d’ does not exist."; exit 1;
    fi
done

sorted_input_files() {
    # If lowram, this skips all paths containing 'RHW'
    find "$INDIR/$1" -type f  \( -name '*.txt' -o -name '*.rul' -o -name '*.ini' \) \
        \! -ipath "$drivesideignore" ${lowram:+ \! -ipath '*RHW*'} -print0 | sort -z
}

concat_before_separator() {
    # `tail -v` adds a header line starting with `==>` which demarcates the
    # start of each file, so this command includes everything between pairs of
    # ==> and separator (and finally deletes lines starting with ==>).
    xargs -0 tail -v -n +1 - | sed -ne '/^==>/,/^\s*;###separator###/p' - | sed -e '/^==>/d' -
}

concat_after_separator() {
    # Deletes everything from ==> to separator, repeatedly.
    xargs -0 tail -v -n +1 - | sed -e '/^==>/,/^\s*;###separator###/d' -
}

strip_drive_side() {
    # Uncomments the RHD code (and leaves LHD code commented out, or vice versa).
    # If an argument is passed, this uses xargs to read file names from stdin,
    # else stdin is read as input directly.
    ${1:+ xargs -0} sed -e "s/;###${driveside}###//g"
}
# Note that we used sed and tail for concatenation which handle missing
# newlines at the end of input files gracefully (unlike cat).

strip_comments() {
    # Not matching whitespace for speed
    sed -e '/^;/d' -
}

uint32_to_bytes() {
    # little endian
    perl -e "print pack('V', $1)"
}

ltext() {
    uint32_to_bytes $(("${#1}" | "0x10000000"))
    printf "$1" | iconv -f utf-8 -t utf-16le
}

# Header
rm -f "$OUT"
printf 'DBPF' >> "$OUT"
printf '\x01\x00\x00\x00\x00\x00\x00\x00' >> "$OUT"  # major.minor = 1.0
for i in {1..21}; do printf '\x00\x00\x00\x00' >> "$OUT"; done  # placeholder

declare -a lengths=()
declare -a offsets=($(wc --bytes < "$OUT"))

for d in "${directories[@]}"
do
    printf "$d..."
    if [ "$d" = "${directories[-1]}" ]; then
        # LText file
        ltext "Cheetah variant $driveside (not for production) compiled in $SECONDS seconds on $(date)" >> "$OUT"
    elif [ "$d" = "${directories[0]}" ]; then
        # RUL0 directory
        sorted_input_files "$d" | concat_before_separator | strip_drive_side >> "$OUT"
        sorted_input_files "$d" | concat_after_separator  | strip_drive_side >> "$OUT"
    else
        # other directories
        sorted_input_files "$d" | strip_drive_side -0 | strip_comments >> "$OUT"
    fi
    offsets+=($(wc --bytes < "$OUT"))
    lengths+=($(("${offsets[-1]}" - "${offsets[-2]}")))
done

# TGI Index
for((i=0, k=0, grp=3; i < "${#tgis[@]}"; i+=grp, k+=1))
do
  tgi=("${tgis[@]:i:grp}")
  for id in "${tgi[@]}"
  do
      uint32_to_bytes "$id" >> "$OUT"
  done
  uint32_to_bytes "${offsets[$k]}" >> "$OUT"
  uint32_to_bytes "${lengths[$k]}" >> "$OUT"
done

# update Header
header=("$(date +%s)"     # creation date
        "$(date +%s)"     # modification date
        "7"               # index version
        "$k"              # number of entries
        "${offsets[-1]}"  # index location
        "$(($k * 20))")   # index length
(for h in "${header[@]}"; do uint32_to_bytes "$h"; done) \
    | dd conv=notrunc status=none bs=4 seek=6 count="${#header[@]}" of="$OUT"

echo "Done."
